package ru.kors.finalproject.controller.api.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.repository.NewsRepository;
import ru.kors.finalproject.service.*;
import ru.kors.finalproject.web.api.v1.ApiPageResponse;
import ru.kors.finalproject.web.api.v1.ApiPageableFactory;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
@Tag(name = "Student Content", description = "News, announcements, notifications, materials, and personal files for students.")
@SecurityRequirement(name = "Bearer")
public class StudentContentV1Controller {

    private final CurrentUserHelper currentUserHelper;
    private final NewsRepository newsRepository;
    private final AnnouncementService announcementService;
    private final NotificationService notificationService;
    private final CourseMaterialService courseMaterialService;
    private final FileLinkService fileLinkService;
    private final FileAssetRepository fileAssetRepository;
    private final ApiPageableFactory apiPageableFactory;

    @GetMapping("/news")
    @Operation(summary = "Get public news feed", description = "Returns news entries visible to students.")
    public ResponseEntity<?> news(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(newsRepository.findByOrderByCreatedAtDesc().stream()
                .map(n -> new NewsDto(
                        n.getId(),
                        n.getTitle(),
                        n.getContent(),
                        n.getCategory() != null ? n.getCategory() : "",
                        n.getCreatedAt() != null ? n.getCreatedAt().toString() : null,
                        resolveNewsImageUrl(n)
                ))
                .toList());
    }

    @GetMapping("/announcements")
    @Operation(summary = "Get section announcements", description = "Returns course announcements for the current student.")
    public ResponseEntity<?> announcements(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        Student student = currentUserHelper.requireStudent(user);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "publishedAt",
                Set.of("publishedAt", "createdAt", "title"));
        var data = announcementService.listForStudent(student, pageable).map(a -> new AnnouncementDto(
                a.getId(), a.getTitle(), a.getContent(),
                a.getSubjectOffering() != null ? a.getSubjectOffering().getId() : null,
                a.getSubjectOffering() != null ? a.getSubjectOffering().getSubject().getCode() : null,
                a.getPublishedAt(), a.isPinned()));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications", description = "Returns student notifications and unread count.")
    public ResponseEntity<?> notifications(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "notifications", notificationService.listForEmail(user.getEmail()).stream()
                        .map(this::toNotificationDto)
                        .toList(),
                "unreadCount", notificationService.unreadCount(user.getEmail())
        ));
    }

    @PostMapping("/notifications/{id}/read")
    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read for the current student.")
    public ResponseEntity<?> markNotificationRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        notificationService.markReadForEmail(id, user.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/notifications/read-all")
    @Operation(summary = "Mark all notifications as read", description = "Marks all notifications as read for the current student.")
    public ResponseEntity<?> markAllNotificationsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllReadForEmail(user.getEmail());
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/materials/{sectionId}")
    @Operation(summary = "Get section materials", description = "Returns published course materials for a section where the student is enrolled.")
    public ResponseEntity<?> courseMaterials(
            @AuthenticationPrincipal User user,
            @PathVariable Long sectionId) {
        Student student = currentUserHelper.requireStudent(user);
        if (!courseMaterialService.isStudentEnrolled(student.getId(), sectionId)) {
            throw new IllegalArgumentException("Not enrolled in this section");
        }
        return ResponseEntity.ok(courseMaterialService.listPublishedForSection(sectionId).stream()
                .map(m -> new MaterialDto(
                        m.getId(), m.getTitle(), m.getDescription(), m.getOriginalFileName(),
                        m.getContentType(), m.getSizeBytes(), m.getCreatedAt(),
                        fileLinkService.createMaterialDownloadUrl(m.getId())
                )).toList());
    }

    @GetMapping("/files")
    @Operation(summary = "Get student files", description = "Returns files owned by the current student with signed download links.")
    public ResponseEntity<?> files(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(fileAssetRepository.findByOwnerStudentIdOrderByUploadedAtDesc(student.getId()).stream()
                .map(f -> new StudentFileDto(
                        f.getId(), f.getOriginalName(), f.getCategory(),
                        f.getContentType(), f.getSizeBytes(), f.getUploadedAt(),
                        fileLinkService.createAssetDownloadUrl(f.getId())
                )).toList());
    }

    private NotificationDto toNotificationDto(Notification n) {
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(),
                n.getMessage(), n.getLink(), n.isRead(), n.getCreatedAt());
    }

    public record AnnouncementDto(Long id, String title, String content, Long sectionId,
                                  String sectionCode, Instant publishedAt, boolean pinned) {}
    public record NewsDto(Long id, String title, String content, String category, String createdAt, String imageUrl) {}
    public record NotificationDto(Long id, Notification.NotificationType type, String title,
                                  String message, String link, boolean read, Instant createdAt) {}
    public record MaterialDto(Long id, String title, String description, String originalFileName,
                              String contentType, long sizeBytes, Instant createdAt, String downloadUrl) {}
    public record StudentFileDto(Long id, String fileName, FileAsset.FileCategory category,
                                 String contentType, long sizeBytes, Instant uploadedAt, String downloadUrl) {}

    private String resolveNewsImageUrl(News news) {
        if (news.getImageStoragePath() == null || news.getImageStoragePath().isBlank()) {
            return null;
        }
        return "/api/v1/public/news/" + news.getId() + "/image";
    }
}
