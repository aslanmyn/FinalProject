package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.kors.finalproject.entity.News;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.repository.NewsRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.service.AnnouncementService;
import ru.kors.finalproject.service.FileLinkService;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public website data such as home content, professors, and public news.")
public class PublicV1Controller {

    private final NewsRepository newsRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final AnnouncementService announcementService;
    private final FileLinkService fileLinkService;

    @GetMapping("/news")
    public ResponseEntity<?> news() {
        List<NewsDto> payload = newsRepository.findByOrderByCreatedAtDesc().stream()
                .map(n -> new NewsDto(
                        n.getId(),
                        n.getTitle(),
                        n.getContent(),
                        n.getCategory() != null ? n.getCategory() : "",
                        n.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/professors")
    public ResponseEntity<?> professors() {
        List<ProfessorListItemDto> payload = teacherRepository.findAllWithFacultyOrderByNameAsc().stream()
                .map(t -> new ProfessorListItemDto(
                        t.getId(),
                        t.getName() != null ? t.getName() : "",
                        t.getDepartment() != null ? t.getDepartment() : "",
                        t.getPositionTitle() != null ? t.getPositionTitle() : "",
                        resolveTeacherPhotoUrl(t),
                        t.getPublicEmail() != null ? t.getPublicEmail() : "",
                        t.getOfficeRoom() != null ? t.getOfficeRoom() : "",
                        t.getOfficeHours() != null ? t.getOfficeHours() : "",
                        t.getFaculty() != null ? t.getFaculty().getName() : "",
                        t.getRole()
                ))
                .toList();
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/professors/{id}")
    public ResponseEntity<?> professorProfile(@PathVariable Long id) {
        Teacher teacher = teacherRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Professor not found"));

        announcementService.publishScheduledAnnouncements();

        List<SectionDto> currentSections = subjectOfferingRepository.findByTeacherIdWithDetails(teacher.getId()).stream()
                .filter(so -> so.getSemester() != null && so.getSemester().isCurrent())
                .map(this::toSectionDto)
                .toList();

        List<AnnouncementDto> announcements = announcementService.listPublicByTeacher(teacher.getId()).stream()
                .map(a -> new AnnouncementDto(
                        a.getId(),
                        a.getTitle(),
                        a.getContent(),
                        a.getSubjectOffering() != null ? a.getSubjectOffering().getId() : null,
                        a.getSubjectOffering() != null && a.getSubjectOffering().getSubject() != null
                                ? a.getSubjectOffering().getSubject().getCode()
                                : null,
                        a.getPublishedAt(),
                        a.isPinned()
                ))
                .toList();

        PublicProfessorProfileDto payload = new PublicProfessorProfileDto(
                teacher.getId(),
                teacher.getName() != null ? teacher.getName() : "",
                teacher.getDepartment() != null ? teacher.getDepartment() : "",
                teacher.getPositionTitle() != null ? teacher.getPositionTitle() : "",
                resolveTeacherPhotoUrl(teacher),
                teacher.getPublicEmail() != null ? teacher.getPublicEmail() : "",
                teacher.getOfficeRoom() != null ? teacher.getOfficeRoom() : "",
                teacher.getOfficeHours() != null ? teacher.getOfficeHours() : "",
                teacher.getBio() != null ? teacher.getBio() : "",
                teacher.getFaculty() != null ? teacher.getFaculty().getName() : "",
                teacher.getRole(),
                currentSections,
                announcements
        );
        return ResponseEntity.ok(payload);
    }

    private SectionDto toSectionDto(SubjectOffering so) {
        return new SectionDto(
                so.getId(),
                so.getSubject() != null ? so.getSubject().getCode() : "",
                so.getSubject() != null ? so.getSubject().getName() : "",
                so.getSemester() != null ? so.getSemester().getName() : "",
                so.getLessonType(),
                so.getDayOfWeek(),
                so.getStartTime(),
                so.getEndTime(),
                so.getRoom() != null ? so.getRoom() : ""
        );
    }

    private String resolveTeacherPhotoUrl(Teacher teacher) {
        if (teacher.getProfilePhotoAssetId() != null) {
            return fileLinkService.createAssetDownloadUrl(teacher.getProfilePhotoAssetId());
        }
        return teacher.getPhotoUrl() != null ? teacher.getPhotoUrl() : "";
    }

    public record NewsDto(Long id, String title, String content, String category, Instant createdAt) {}

    public record ProfessorListItemDto(
            Long id,
            String name,
            String department,
            String positionTitle,
            String photoUrl,
            String publicEmail,
            String officeRoom,
            String officeHours,
            String faculty,
            Teacher.TeacherRole role
    ) {}

    public record PublicProfessorProfileDto(
            Long id,
            String name,
            String department,
            String positionTitle,
            String photoUrl,
            String publicEmail,
            String officeRoom,
            String officeHours,
            String bio,
            String faculty,
            Teacher.TeacherRole role,
            List<SectionDto> currentSections,
            List<AnnouncementDto> announcements
    ) {}

    public record SectionDto(
            Long id,
            String subjectCode,
            String subjectName,
            String semesterName,
            SubjectOffering.LessonType lessonType,
            java.time.DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String room
    ) {}

    public record AnnouncementDto(
            Long id,
            String title,
            String content,
            Long sectionId,
            String sectionCode,
            Instant publishedAt,
            boolean pinned
    ) {}
}
