package ru.kors.finalproject.controller.api.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.RequestService;
import ru.kors.finalproject.web.api.v1.ApiPageResponse;
import ru.kors.finalproject.web.api.v1.ApiPageableFactory;
import ru.kors.finalproject.web.api.v1.CurrentUserHelper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentServicesV1Controller {

    private final CurrentUserHelper currentUserHelper;
    private final ChecklistItemRepository checklistItemRepository;
    private final MobilityApplicationRepository mobilityApplicationRepository;
    private final ClearanceSheetRepository clearanceSheetRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final RequestService requestService;
    private final ApiPageableFactory apiPageableFactory;

    @GetMapping("/checklist")
    public ResponseEntity<?> checklist(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(checklistItemRepository.findByStudentIdOrderByDeadlineAsc(student.getId()).stream()
                .map(item -> new ChecklistItemDto(
                        item.getId(), item.getTitle(), item.getDeadline(),
                        item.isCompleted(), item.getLinkToSection()))
                .toList());
    }

    @GetMapping("/mobility")
    public ResponseEntity<?> mobility(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        return ResponseEntity.ok(mobilityApplicationRepository
                .findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId()).stream()
                .map(a -> new MobilityDto(
                        a.getId(),
                        a.getStudent() != null ? a.getStudent().getId() : null,
                        a.getUniversityName(),
                        a.getDisciplinesMapping(),
                        a.getStatus(),
                        a.getCreatedAt()))
                .toList());
    }

    @GetMapping("/clearance")
    public ResponseEntity<?> clearance(@AuthenticationPrincipal User user) {
        Student student = currentUserHelper.requireStudent(user);
        ClearanceSheet sheet = clearanceSheetRepository.findByStudentIdWithDetails(student.getId()).orElse(null);
        return ResponseEntity.ok(sheet == null
                ? new ClearanceDto(null, student.getId(), student.getName(), null, List.of())
                : toClearanceDto(sheet));
    }

    @GetMapping("/requests")
    public ResponseEntity<?> requests(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        Student student = currentUserHelper.requireStudent(user);
        var pageable = apiPageableFactory.create(
                page, size, sort, direction, "createdAt",
                Set.of("createdAt", "updatedAt", "category", "status"));
        var data = studentRequestRepository.findByStudentId(student.getId(), pageable)
                .map(r -> new RequestDto(r.getId(), r.getCategory(), r.getDescription(),
                        r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()));
        return ResponseEntity.ok(ApiPageResponse.from(data));
    }

    @PostMapping("/requests")
    public ResponseEntity<?> createRequest(
            @AuthenticationPrincipal User user,
            @RequestBody CreateRequestBody body) {
        Student student = currentUserHelper.requireStudent(user);
        StudentRequest request = requestService.createRequest(student, body.category(), body.description());
        return ResponseEntity.ok(new RequestDto(request.getId(), request.getCategory(), request.getDescription(),
                request.getStatus(), request.getCreatedAt(), request.getUpdatedAt()));
    }

    @GetMapping("/requests/{id}/messages")
    public ResponseEntity<?> requestMessages(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        Student student = currentUserHelper.requireStudent(user);
        StudentRequest req = studentRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        return ResponseEntity.ok(requestService.getMessages(id).stream()
                .map(m -> new RequestMessageDto(
                        m.getId(),
                        m.getSender() != null ? m.getSender().getId() : null,
                        m.getSender() != null ? m.getSender().getEmail() : null,
                        m.getSender() != null ? m.getSender().getFullName() : null,
                        m.getMessage(),
                        m.getCreatedAt()))
                .toList());
    }

    @PostMapping("/requests/{id}/messages")
    public ResponseEntity<?> addRequestMessage(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody AddMessageBody body) {
        Student student = currentUserHelper.requireStudent(user);
        StudentRequest req = studentRequestRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));
        if (!req.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("Access denied");
        }
        RequestMessage msg = requestService.addMessage(id, user, body.message());
        return ResponseEntity.ok(new RequestMessageDto(
                msg.getId(),
                msg.getSender() != null ? msg.getSender().getId() : null,
                msg.getSender() != null ? msg.getSender().getEmail() : null,
                msg.getSender() != null ? msg.getSender().getFullName() : null,
                msg.getMessage(),
                msg.getCreatedAt()));
    }

    private ClearanceDto toClearanceDto(ClearanceSheet sheet) {
        return new ClearanceDto(
                sheet.getId(),
                sheet.getStudent() != null ? sheet.getStudent().getId() : null,
                sheet.getStudent() != null ? sheet.getStudent().getName() : null,
                sheet.getStatus(),
                sheet.getCheckpoints().stream()
                        .map(cp -> new ClearanceCheckpointDto(cp.getId(), cp.getDepartment(),
                                cp.getStatus(), cp.getComment()))
                        .toList()
        );
    }

    public record ChecklistItemDto(Long id, String title, LocalDate deadline,
                                   boolean completed, String linkToSection) {}
    public record MobilityDto(Long id, Long studentId, String universityName,
                              String disciplinesMapping, MobilityApplication.MobilityStatus status,
                              Instant createdAt) {}
    public record ClearanceDto(Long id, Long studentId, String studentName,
                               ClearanceSheet.ClearanceStatus status, List<ClearanceCheckpointDto> checkpoints) {}
    public record ClearanceCheckpointDto(Long id, String department,
                                         ClearanceCheckpoint.CheckpointStatus status, String comment) {}
    public record RequestDto(Long id, String category, String description,
                             StudentRequest.RequestStatus status, Instant createdAt, Instant updatedAt) {}
    public record RequestMessageDto(Long id, Long senderUserId, String senderEmail,
                                    String senderName, String message, Instant createdAt) {}
    public record CreateRequestBody(String category, String description) {}
    public record AddMessageBody(String message) {}
}
