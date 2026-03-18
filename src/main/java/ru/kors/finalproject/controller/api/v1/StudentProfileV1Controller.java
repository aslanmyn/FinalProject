package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.service.FileLinkService;
import ru.kors.finalproject.service.FileStorageService;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentProfileV1Controller {

    private final CurrentUserHelper currentUserHelper;
    private final FileAssetRepository fileAssetRepository;
    private final FileStorageService fileStorageService;
    private final FileLinkService fileLinkService;

    @GetMapping("/profile")
    public ResponseEntity<?> profile(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toDto(student));
    }

    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePhoto(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) {
        Student student = currentUserHelper.requireStudent(user);
        if (file.getContentType() == null || !file.getContentType().toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed for profile photo");
        }

        FileStorageService.StoredFile stored = fileStorageService.store(file, "profile-photos/student-" + student.getId());
        FileAsset previousAsset = student.getProfilePhotoAssetId() != null
                ? fileAssetRepository.findById(student.getProfilePhotoAssetId()).orElse(null)
                : null;

        FileAsset savedAsset = fileAssetRepository.save(FileAsset.builder()
                .originalName(stored.originalName())
                .storagePath(stored.storagePath())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .category(FileAsset.FileCategory.OTHER)
                .linkedEntityType("STUDENT_PROFILE_PHOTO")
                .linkedEntityId(student.getId())
                .ownerStudent(student)
                .uploadedBy(user)
                .uploadedAt(Instant.now())
                .build());

        student.setProfilePhotoAssetId(savedAsset.getId());
        currentUserHelper.saveStudent(student);

        if (previousAsset != null) {
            fileStorageService.deleteSilently(previousAsset.getStoragePath());
            fileAssetRepository.delete(previousAsset);
        }

        return ResponseEntity.ok(toDto(student));
    }

    private StudentProfileDto toDto(Student student) {
        String photoUrl = student.getProfilePhotoAssetId() != null
                ? fileAssetRepository.findById(student.getProfilePhotoAssetId())
                        .map(f -> fileLinkService.createAssetDownloadUrl(f.getId()))
                        .orElse(null)
                : null;
        return new StudentProfileDto(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getCourse(),
                student.getGroupName(),
                student.getStatus(),
                student.getProgram() != null ? student.getProgram().getName() : null,
                student.getFaculty() != null ? student.getFaculty().getName() : null,
                student.getCreditsEarned(),
                student.getPhone(),
                photoUrl
        );
    }

    public record StudentProfileDto(Long id, String name, String email, int course, String groupName,
                                    Student.StudentStatus status, String program, String faculty,
                                    int creditsEarned, String phone, String profilePhotoUrl) {}
}
