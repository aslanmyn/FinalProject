package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.CourseMaterialRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseMaterialService {

    private final CourseMaterialRepository courseMaterialRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    @Transactional
    public CourseMaterial upload(Teacher teacher, Long sectionId, String title, String description,
                                 String originalFileName, String storagePath,
                                 String contentType, long sizeBytes,
                                 CourseMaterial.MaterialVisibility visibility) {
        SubjectOffering section = getTeacherSection(teacher, sectionId);
        return persistMaterial(section, teacher, title, description, originalFileName, storagePath, contentType, sizeBytes, visibility);
    }

    @Transactional
    public CourseMaterial upload(
            Teacher teacher,
            Long sectionId,
            String title,
            String description,
            MultipartFile file,
            CourseMaterial.MaterialVisibility visibility) {
        SubjectOffering section = getTeacherSection(teacher, sectionId);
        FileStorageService.StoredFile stored = fileStorageService.store(file, "materials/section-" + sectionId);
        return persistMaterial(
                section,
                teacher,
                title,
                description,
                stored.originalName(),
                stored.storagePath(),
                stored.contentType(),
                stored.sizeBytes(),
                visibility
        );
    }

    private CourseMaterial persistMaterial(
            SubjectOffering section,
            Teacher teacher,
            String title,
            String description,
            String originalFileName,
            String storagePath,
            String contentType,
            long sizeBytes,
            CourseMaterial.MaterialVisibility visibility) {
        CourseMaterial material = CourseMaterial.builder()
                .subjectOffering(section)
                .uploadedBy(teacher)
                .title(title)
                .description(description)
                .originalFileName(originalFileName)
                .storagePath(storagePath)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .visibility(visibility)
                .published(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        CourseMaterial saved = courseMaterialRepository.save(material);

        List<Registration> enrolled = registrationRepository.findBySubjectOfferingIdAndStatusIn(
                section.getId(), List.of(Registration.RegistrationStatus.CONFIRMED));
        for (Registration reg : enrolled) {
            notificationService.notifyStudent(
                    reg.getStudent().getEmail(),
                    Notification.NotificationType.SYSTEM,
                    "New course material",
                    "New material posted in " + section.getSubject().getCode() + ": " + title,
                    "/app/student/journal"
            );
        }
        return saved;
    }

    @Transactional
    public CourseMaterial updateVisibility(Teacher teacher, Long materialId, boolean published) {
        CourseMaterial material = courseMaterialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));
        getTeacherSection(teacher, material.getSubjectOffering().getId());
        material.setPublished(published);
        material.setUpdatedAt(Instant.now());
        return courseMaterialRepository.save(material);
    }

    @Transactional
    public void delete(Teacher teacher, Long materialId) {
        CourseMaterial material = courseMaterialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));
        getTeacherSection(teacher, material.getSubjectOffering().getId());
        fileStorageService.deleteSilently(material.getStoragePath());
        courseMaterialRepository.delete(material);
    }

    public List<CourseMaterial> listForSection(Teacher teacher, Long sectionId) {
        getTeacherSection(teacher, sectionId);
        return courseMaterialRepository.findBySubjectOfferingIdWithDetailsOrderByCreatedAtDesc(sectionId);
    }

    public List<CourseMaterial> listPublishedForSection(Long sectionId) {
        return courseMaterialRepository.findBySubjectOfferingIdAndPublishedTrueOrderByCreatedAtDesc(sectionId);
    }

    public boolean isStudentEnrolled(Long studentId, Long sectionId) {
        return registrationRepository.findByStudentIdAndSubjectOfferingId(studentId, sectionId)
                .filter(r -> r.getStatus() == Registration.RegistrationStatus.CONFIRMED)
                .isPresent();
    }

    private SubjectOffering getTeacherSection(Teacher teacher, Long sectionId) {
        SubjectOffering section = subjectOfferingRepository.findByIdWithDetails(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (section.getTeacher() == null || !section.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Section is not assigned to current teacher");
        }
        return section;
    }
}
