package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.CourseMaterialRepository;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.service.FileLinkService;
import ru.kors.finalproject.service.FileStorageService;
import ru.kors.finalproject.web.api.v1.ApiForbiddenException;
import ru.kors.finalproject.web.api.v1.ApiUnauthorizedException;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileV1Controller {
    private final FileAssetRepository fileAssetRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final RegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final FileLinkService fileLinkService;
    private final FileStorageService fileStorageService;

    @GetMapping("/asset/{id}/link")
    public ResponseEntity<?> generateAssetLink(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        FileAsset asset = fileAssetRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("File asset not found"));
        assertCanAccessAsset(user, asset);
        return ResponseEntity.ok(new LinkResponse(fileLinkService.createAssetDownloadUrl(asset.getId())));
    }

    @GetMapping("/material/{id}/link")
    public ResponseEntity<?> generateMaterialLink(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        CourseMaterial material = courseMaterialRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Course material not found"));
        assertCanAccessMaterial(user, material);
        return ResponseEntity.ok(new LinkResponse(fileLinkService.createMaterialDownloadUrl(material.getId())));
    }

    @GetMapping("/download/asset/{id}")
    public ResponseEntity<?> downloadAsset(
            @PathVariable Long id,
            @RequestParam long exp,
            @RequestParam String sig) {
        if (!fileLinkService.isValidAssetSignature(id, exp, sig)) {
            throw new ApiUnauthorizedException("Invalid or expired download link");
        }
        FileAsset asset = fileAssetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("File asset not found"));
        return fileStorageService.buildDownloadResponse(
                asset.getStoragePath(),
                asset.getOriginalName(),
                asset.getContentType()
        );
    }

    @GetMapping("/download/material/{id}")
    public ResponseEntity<?> downloadMaterial(
            @PathVariable Long id,
            @RequestParam long exp,
            @RequestParam String sig) {
        if (!fileLinkService.isValidMaterialSignature(id, exp, sig)) {
            throw new ApiUnauthorizedException("Invalid or expired download link");
        }
        CourseMaterial material = courseMaterialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course material not found"));
        return fileStorageService.buildDownloadResponse(
                material.getStoragePath(),
                material.getOriginalFileName(),
                material.getContentType()
        );
    }

    private void assertCanAccessAsset(User user, FileAsset asset) {
        if (user.getRole() == User.UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == User.UserRole.STUDENT) {
            if (asset.getOwnerStudent() != null && user.getEmail().equalsIgnoreCase(asset.getOwnerStudent().getEmail())) {
                return;
            }
            throw new ApiForbiddenException("You can only access your own files");
        }
        if (user.getRole() == User.UserRole.PROFESSOR) {
            if (asset.getUploadedBy() != null && user.getId().equals(asset.getUploadedBy().getId())) {
                return;
            }
            throw new ApiForbiddenException("Teacher can access only self-uploaded student files");
        }
        throw new ApiForbiddenException("Access denied");
    }

    private void assertCanAccessMaterial(User user, CourseMaterial material) {
        if (user.getRole() == User.UserRole.ADMIN) {
            return;
        }
        if (user.getRole() == User.UserRole.PROFESSOR) {
            if (material.getSubjectOffering() != null
                    && material.getSubjectOffering().getTeacher() != null
                    && user.getEmail().equalsIgnoreCase(material.getSubjectOffering().getTeacher().getEmail())) {
                return;
            }
            throw new ApiForbiddenException("Teacher can access only own section materials");
        }
        if (user.getRole() == User.UserRole.STUDENT) {
            Student student = studentRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new ApiForbiddenException("Student profile not found"));
            boolean enrolled = material.getSubjectOffering() != null
                    && registrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), material.getSubjectOffering().getId())
                    .filter(r -> r.getStatus() == Registration.RegistrationStatus.CONFIRMED
                            || r.getStatus() == Registration.RegistrationStatus.SUBMITTED)
                    .isPresent();
            if (material.getVisibility() == CourseMaterial.MaterialVisibility.PUBLIC && material.isPublished()) {
                return;
            }
            if (enrolled && material.isPublished()) {
                return;
            }
            throw new ApiForbiddenException("Material is not available for this student");
        }
        throw new ApiForbiddenException("Access denied");
    }

    public record LinkResponse(String downloadUrl) {
    }
}
