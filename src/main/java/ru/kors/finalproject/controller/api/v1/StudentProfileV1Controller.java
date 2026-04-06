package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Student Profile", description = "Student profile data and profile photo upload.")
@SecurityRequirement(name = "Bearer")
public class StudentProfileV1Controller {

    private final CurrentUserHelper currentUserHelper;
    private final FileAssetRepository fileAssetRepository;
    private final FileStorageService fileStorageService;
    private final FileLinkService fileLinkService;

    @GetMapping("/profile")
    @Operation(summary = "Get student profile", description = "Returns the current student's profile card data.")
    public ResponseEntity<?> profile(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(toDto(student));
    }

    @PutMapping("/profile")
    @Operation(
            summary = "Update student profile",
            description = "Updates student-owned contact data such as phone, address, and emergency contact without changing academic assignment fields."
    )
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody StudentProfileUpdateRequest request
    ) {
        Student student = currentUserHelper.requireStudent(user);
        student.setPhone(normalize(request.phone(), 50));
        student.setAddress(normalize(request.address(), 255));
        student.setEmergencyContact(normalize(request.emergencyContact(), 255));
        currentUserHelper.saveStudent(student);
        return ResponseEntity.ok(toDto(student));
    }

    @PostMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload student profile photo", description = "Uploads a new student avatar image and replaces the previous one.")
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
                student.getAddress(),
                student.getEmergencyContact(),
                photoUrl
        );
    }

    private String normalize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    public record StudentProfileUpdateRequest(String phone, String address, String emergencyContact) {}

    public record StudentProfileDto(Long id, String name, String email, int course, String groupName,
                                    Student.StudentStatus status, String program, String faculty,
                                    int creditsEarned, String phone, String address,
                                    String emergencyContact, String profilePhotoUrl) {}
}
