package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.GradeChangeRequestRepository;
import ru.kors.finalproject.repository.GradeRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GradeChangeService {
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final GradeRepository gradeRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public GradeChangeRequest createForComponentGrade(
            Teacher teacher,
            Long subjectOfferingId,
            Long gradeId,
            double newValue,
            String reason) {
        SubjectOffering offering = subjectOfferingRepository.findById(subjectOfferingId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (offering.getTeacher() == null || !offering.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Section is not assigned to this teacher");
        }
        Grade grade = gradeRepository.findById(gradeId).orElseThrow(() -> new IllegalArgumentException("Grade not found"));
        if (!grade.getSubjectOffering().getId().equals(subjectOfferingId)) {
            throw new IllegalArgumentException("Grade does not belong to section");
        }

        GradeChangeRequest request = GradeChangeRequest.builder()
                .teacher(teacher)
                .student(grade.getStudent())
                .subjectOffering(offering)
                .grade(grade)
                .oldValue(grade.getGradeValue())
                .newValue(newValue)
                .reason(reason)
                .status(GradeChangeRequest.RequestStatus.SUBMITTED)
                .createdAt(Instant.now())
                .build();
        GradeChangeRequest saved = gradeChangeRequestRepository.save(request);
        notificationService.notifyStudent(
                grade.getStudent().getEmail(),
                Notification.NotificationType.REQUEST,
                "Grade change request submitted",
                "A grade change request was submitted for " + offering.getSubject().getCode(),
                "/app/student/journal"
        );
        notificationService.notifyUsers(
                userRepository.findByRoleAndEnabledTrue(User.UserRole.ADMIN),
                Notification.NotificationType.REQUEST,
                "Grade change request submitted",
                "Teacher " + teacher.getName() + " submitted a grade change request for "
                        + offering.getSubject().getCode(),
                "/app/admin/moderation"
        );
        auditService.logStudentAction(null, "GRADE_CHANGE_REQUEST_CREATED", "GradeChangeRequest", saved.getId(),
                "gradeId=" + gradeId + ", old=" + grade.getGradeValue() + ", new=" + newValue);
        return saved;
    }

    @Transactional
    public GradeChangeRequest review(Long requestId, boolean approve, String reviewerComment, User admin) {
        GradeChangeRequest request = gradeChangeRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (request.getStatus() != GradeChangeRequest.RequestStatus.SUBMITTED) {
            throw new IllegalStateException("Only submitted requests can be reviewed");
        }
        request.setStatus(approve ? GradeChangeRequest.RequestStatus.APPROVED : GradeChangeRequest.RequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(admin);
        request.setReviewerComment(reviewerComment);
        GradeChangeRequest saved = gradeChangeRequestRepository.save(request);

        if (approve) {
            applyApproved(saved, admin);
        } else {
            notificationService.notifyStudent(
                    request.getStudent().getEmail(),
                    Notification.NotificationType.REQUEST,
                    "Grade change request rejected",
                    "Your teacher's grade change request was rejected by registrar",
                    "/app/student/journal"
            );
            auditService.logUserAction(admin, "GRADE_CHANGE_REQUEST_REJECTED", "GradeChangeRequest", requestId,
                    "comment=" + reviewerComment);
        }
        return saved;
    }

    @Transactional
    protected void applyApproved(GradeChangeRequest request, User admin) {
        if (request.getGrade() != null) {
            Grade grade = gradeRepository.findById(request.getGrade().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Grade not found"));
            grade.setGradeValue(request.getNewValue());
            gradeRepository.save(grade);
        }
        request.setStatus(GradeChangeRequest.RequestStatus.APPLIED);
        request.setAppliedAt(Instant.now());
        gradeChangeRequestRepository.save(request);

        notificationService.notifyStudent(
                request.getStudent().getEmail(),
                Notification.NotificationType.GRADE,
                "Grade changed",
                "A grade was changed by approved registrar request",
                "/app/student/journal"
        );
        auditService.logUserAction(admin, "GRADE_CHANGE_REQUEST_APPLIED", "GradeChangeRequest", request.getId(),
                "newValue=" + request.getNewValue());
    }

    public List<GradeChangeRequest> listForTeacher(Teacher teacher) {
        return gradeChangeRequestRepository.findByTeacherIdWithDetailsOrderByCreatedAtDesc(teacher.getId());
    }

    public List<GradeChangeRequest> listPending() {
        return gradeChangeRequestRepository.findByStatusWithDetailsOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus.SUBMITTED);
    }
}
