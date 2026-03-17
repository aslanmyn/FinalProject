package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.FileAssetRepository;
import ru.kors.finalproject.repository.RequestMessageRepository;
import ru.kors.finalproject.repository.StudentRequestRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final StudentRequestRepository studentRequestRepository;
    private final RequestMessageRepository requestMessageRepository;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Transactional
    public StudentRequest createRequest(Student student, String category, String description) {
        StudentRequest request = StudentRequest.builder()
                .student(student)
                .category(category)
                .description(description)
                .status(StudentRequest.RequestStatus.NEW)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        StudentRequest saved = studentRequestRepository.save(request);
        notificationService.notifyUsers(
                userRepository.findByRoleAndEnabledTrue(User.UserRole.ADMIN),
                Notification.NotificationType.REQUEST,
                "New student request",
                "Request #" + saved.getId() + " was created in category " + category,
                "/app/admin/requests"
        );
        auditService.logStudentAction(student, "REQUEST_CREATED", "StudentRequest", saved.getId(), "category=" + category);
        return saved;
    }

    @Transactional
    public RequestMessage addMessage(Long requestId, User sender, String message) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (sender.getRole() == User.UserRole.STUDENT) {
            if (request.getStudent() == null || !sender.getEmail().equalsIgnoreCase(request.getStudent().getEmail())) {
                throw new IllegalArgumentException("Student can only write to own requests");
            }
        }
        RequestMessage requestMessage = RequestMessage.builder()
                .request(request)
                .sender(sender)
                .message(message)
                .createdAt(Instant.now())
                .build();
        RequestMessage saved = requestMessageRepository.save(requestMessage);
        request.setUpdatedAt(Instant.now());
        studentRequestRepository.save(request);

        if (request.getStudent() != null && request.getStudent().getEmail() != null
                && !request.getStudent().getEmail().equalsIgnoreCase(sender.getEmail())) {
            notificationService.notifyStudent(
                    request.getStudent().getEmail(),
                    Notification.NotificationType.REQUEST,
                    "New response for request",
                    "There is a new message in request #" + request.getId(),
                    "/app/student/requests"
            );
        }
        if (request.getAssignedTo() != null && (sender == null || !request.getAssignedTo().getId().equals(sender.getId()))) {
            notificationService.notifyUser(
                    request.getAssignedTo(),
                    Notification.NotificationType.REQUEST,
                    "Request updated",
                    "There is a new message in request #" + request.getId(),
                    "/app/admin/requests"
            );
        }
        auditService.logUserAction(sender, "REQUEST_MESSAGE_ADDED", "RequestMessage", saved.getId(), "requestId=" + requestId);
        return saved;
    }

    @Transactional
    public StudentRequest updateStatus(Long requestId, StudentRequest.RequestStatus status, User actor) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        request.setStatus(status);
        request.setUpdatedAt(Instant.now());
        if (status == StudentRequest.RequestStatus.DONE || status == StudentRequest.RequestStatus.REJECTED) {
            request.setClosedAt(Instant.now());
        }
        StudentRequest saved = studentRequestRepository.save(request);
        if (saved.getStudent() != null) {
            notificationService.notifyStudent(
                    saved.getStudent().getEmail(),
                    Notification.NotificationType.REQUEST,
                    "Request status updated",
                    "Request #" + saved.getId() + " status: " + status,
                    "/app/student/requests"
            );
        }
        auditService.logUserAction(actor, "REQUEST_STATUS_UPDATED", "StudentRequest", requestId, "status=" + status);
        return saved;
    }

    @Transactional
    public StudentRequest assign(Long requestId, Long adminUserId, User actor) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        User assignee = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        request.setAssignedTo(assignee);
        request.setStatus(StudentRequest.RequestStatus.IN_REVIEW);
        request.setUpdatedAt(Instant.now());
        StudentRequest saved = studentRequestRepository.save(request);
        notificationService.notifyUser(
                assignee,
                Notification.NotificationType.REQUEST,
                "Request assigned",
                "Request #" + saved.getId() + " was assigned to you",
                "/app/admin/requests"
        );
        auditService.logUserAction(actor, "REQUEST_ASSIGNED", "StudentRequest", requestId, "assigneeUserId=" + adminUserId);
        return saved;
    }

    @Transactional
    public FileAsset attachFile(
            Long requestId,
            User uploader,
            String originalName,
            String storagePath,
            String contentType,
            long sizeBytes,
            FileAsset.FileCategory category) {
        StudentRequest request = studentRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        // Ownership check: only the request's student, the assigned admin, or any admin may attach files.
        boolean isOwnerStudent = request.getStudent() != null
                && uploader.getEmail().equalsIgnoreCase(request.getStudent().getEmail());
        boolean isAssignedAdmin = request.getAssignedTo() != null
                && uploader.getId().equals(request.getAssignedTo().getId());
        boolean isAdmin = uploader.getRole() == User.UserRole.ADMIN;

        if (!isOwnerStudent && !isAssignedAdmin && !isAdmin) {
            throw new IllegalArgumentException("You do not have permission to attach files to this request");
        }

        FileAsset fileAsset = FileAsset.builder()
                .originalName(originalName)
                .storagePath(storagePath)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .category(category)
                .linkedEntityType("StudentRequest")
                .linkedEntityId(request.getId())
                .ownerStudent(request.getStudent())
                .uploadedBy(uploader)
                .uploadedAt(Instant.now())
                .build();
        FileAsset saved = fileAssetRepository.save(fileAsset);
        auditService.logUserAction(uploader, "REQUEST_ATTACHMENT_ADDED", "FileAsset", saved.getId(), "requestId=" + requestId);
        return saved;
    }

    public List<RequestMessage> getMessages(Long requestId) {
        return requestMessageRepository.findByRequestIdWithSenderOrderByCreatedAtAsc(requestId);
    }

    public List<FileAsset> getAttachments(Long requestId) {
        return fileAssetRepository.findByLinkedEntityTypeAndLinkedEntityIdOrderByUploadedAtDesc("StudentRequest", requestId);
    }
}
