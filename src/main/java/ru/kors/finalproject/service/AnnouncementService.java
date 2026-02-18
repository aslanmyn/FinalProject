package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.CourseAnnouncementRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final CourseAnnouncementRepository announcementRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public CourseAnnouncement createAnnouncement(
            Teacher teacher,
            Long subjectOfferingId,
            String title,
            String content,
            boolean publicVisible,
            boolean pinned,
            Instant scheduledAt) {
        SubjectOffering offering = null;
        if (subjectOfferingId != null) {
            offering = subjectOfferingRepository.findById(subjectOfferingId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found"));
            if (offering.getTeacher() == null || !offering.getTeacher().getId().equals(teacher.getId())) {
                throw new IllegalArgumentException("Section is not assigned to this teacher");
            }
        }
        boolean publishNow = scheduledAt == null || !scheduledAt.isAfter(Instant.now());
        CourseAnnouncement announcement = CourseAnnouncement.builder()
                .teacher(teacher)
                .subjectOffering(offering)
                .title(title)
                .content(content)
                .publicVisible(publicVisible)
                .pinned(pinned)
                .scheduledAt(scheduledAt)
                .published(publishNow)
                .publishedAt(publishNow ? Instant.now() : null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        CourseAnnouncement saved = announcementRepository.save(announcement);
        if (publishNow) {
            notifyStudents(saved);
        }
        auditService.logStudentAction(null, "ANNOUNCEMENT_CREATED", "CourseAnnouncement", saved.getId(),
                "teacherId=" + teacher.getId() + ", sectionId=" + (offering != null ? offering.getId() : null));
        return saved;
    }

    @Transactional
    public void publishScheduledAnnouncements() {
        List<CourseAnnouncement> toPublish = announcementRepository.findByPublishedFalseAndScheduledAtBefore(Instant.now());
        for (CourseAnnouncement announcement : toPublish) {
            announcement.setPublished(true);
            announcement.setPublishedAt(Instant.now());
            announcement.setUpdatedAt(Instant.now());
            announcementRepository.save(announcement);
            notifyStudents(announcement);
        }
    }

    public List<CourseAnnouncement> listPublicByTeacher(Long teacherId) {
        return announcementRepository.findByTeacherIdAndPublicVisibleTrueAndPublishedTrueWithDetails(teacherId);
    }

    public List<CourseAnnouncement> listForTeacher(Teacher teacher) {
        return announcementRepository.findByTeacherIdWithDetailsOrderByPinnedDescPublishedAtDesc(teacher.getId());
    }

    public List<CourseAnnouncement> listForSection(Teacher teacher, Long sectionId) {
        SubjectOffering section = subjectOfferingRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (section.getTeacher() == null || !section.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Section is not assigned to this teacher");
        }
        return announcementRepository.findBySubjectOfferingIdOrderByPinnedDescPublishedAtDesc(sectionId);
    }

    public List<CourseAnnouncement> listForStudent(Student student) {
        List<Long> offeringIds = registrationRepository.findActiveByStudentIdWithDetails(student.getId()).stream()
                .map(r -> r.getSubjectOffering().getId())
                .toList();
        if (offeringIds.isEmpty()) {
            return List.of();
        }
        return announcementRepository.findPublishedForOfferingIdsWithDetailsOrderByPinnedDescPublishedAtDesc(offeringIds);
    }

    public Page<CourseAnnouncement> listForStudent(Student student, Pageable pageable) {
        List<Long> offeringIds = registrationRepository.findActiveByStudentIdWithDetails(student.getId()).stream()
                .map(r -> r.getSubjectOffering().getId())
                .toList();
        if (offeringIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return announcementRepository.findPublishedForOfferingIdsWithDetails(offeringIds, pageable);
    }

    private void notifyStudents(CourseAnnouncement announcement) {
        if (announcement.getSubjectOffering() == null) {
            return;
        }
        var enrollments = registrationRepository.findBySubjectOfferingIdAndStatusIn(
                announcement.getSubjectOffering().getId(),
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
        );
        for (Registration enrollment : enrollments) {
            notificationService.notifyStudent(
                    enrollment.getStudent().getEmail(),
                    Notification.NotificationType.SYSTEM,
                    "New course announcement",
                    announcement.getTitle(),
                    "/portal/student-schedule"
            );
        }
    }
}
