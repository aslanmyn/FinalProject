package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorkflowEngineService {
    private final StudentRequestRepository studentRequestRepository;
    private final FxRegistrationRepository fxRegistrationRepository;
    private final MobilityApplicationRepository mobilityApplicationRepository;
    private final ClearanceSheetRepository clearanceSheetRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final AuditLogRepository auditLogRepository;

    public void assertRequestTransition(StudentRequest.RequestStatus current, StudentRequest.RequestStatus next) {
        if (!allowedRequestTransitions(current).contains(next)) {
            throw new IllegalStateException("Invalid request transition from " + current + " to " + next);
        }
    }

    public void assertFxTransition(FxRegistration.FxStatus current, FxRegistration.FxStatus next) {
        if (!allowedFxTransitions(current).contains(next)) {
            throw new IllegalStateException("Invalid FX transition from " + current + " to " + next);
        }
    }

    public void assertMobilityTransition(MobilityApplication.MobilityStatus current, MobilityApplication.MobilityStatus next) {
        if (!allowedMobilityTransitions(current).contains(next)) {
            throw new IllegalStateException("Invalid mobility transition from " + current + " to " + next);
        }
    }

    public void assertGradeChangeTransition(GradeChangeRequest.RequestStatus current, GradeChangeRequest.RequestStatus next) {
        if (!allowedGradeChangeTransitions(current).contains(next)) {
            throw new IllegalStateException("Invalid grade change transition from " + current + " to " + next);
        }
    }

    public void assertRegistrationTransition(Registration.RegistrationStatus current, Registration.RegistrationStatus next) {
        if (!allowedRegistrationTransitions(current).contains(next)) {
            throw new IllegalStateException("Invalid registration transition from " + current + " to " + next);
        }
    }

    public void assertClearanceCheckpointTransition(ClearanceCheckpoint.CheckpointStatus current, ClearanceCheckpoint.CheckpointStatus next) {
        if (!allowedClearanceCheckpointTransitions(current).contains(next)) {
            throw new IllegalStateException("Invalid clearance checkpoint transition from " + current + " to " + next);
        }
    }

    @Transactional(readOnly = true)
    public WorkflowOverview buildStudentOverview(Student student) {
        List<WorkflowItem> items = new ArrayList<>();

        studentRequestRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId()).stream()
                .filter(request -> request.getStatus() != StudentRequest.RequestStatus.DONE)
                .forEach(request -> items.add(new WorkflowItem(
                        WorkflowType.REQUEST,
                        request.getId(),
                        request.getCategory() != null ? request.getCategory() : "Request",
                        "Student request",
                        request.getStatus().name(),
                        request.getCreatedAt(),
                        request.getUpdatedAt(),
                        deadline(request.getCreatedAt(), 3),
                        isOverdue(deadline(request.getCreatedAt(), 3), request.getStatus().name(), List.of("DONE", "REJECTED")),
                        allowedRequestTransitions(request.getStatus()).stream().map(Enum::name).toList(),
                        "/app/student/requests"
                )));

        fxRegistrationRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId()).stream()
                .filter(fx -> fx.getStatus() != FxRegistration.FxStatus.CONFIRMED)
                .forEach(fx -> items.add(new WorkflowItem(
                        WorkflowType.FX,
                        fx.getId(),
                        fx.getSubjectOffering() != null && fx.getSubjectOffering().getSubject() != null
                                ? fx.getSubjectOffering().getSubject().getCode()
                                : "FX",
                        "FX registration",
                        fx.getStatus().name(),
                        fx.getCreatedAt(),
                        fx.getCreatedAt(),
                        resolveWindowDeadline(fx.getSubjectOffering(), RegistrationWindow.WindowType.FX, fx.getCreatedAt(), 7),
                        isOverdue(resolveWindowDeadline(fx.getSubjectOffering(), RegistrationWindow.WindowType.FX, fx.getCreatedAt(), 7),
                                fx.getStatus().name(), List.of("CONFIRMED")),
                        allowedFxTransitions(fx.getStatus()).stream().map(Enum::name).toList(),
                        "/app/student/registration"
                )));

        mobilityApplicationRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId()).stream()
                .filter(application -> application.getStatus() != MobilityApplication.MobilityStatus.APPROVED
                        && application.getStatus() != MobilityApplication.MobilityStatus.REJECTED)
                .forEach(application -> items.add(new WorkflowItem(
                        WorkflowType.MOBILITY,
                        application.getId(),
                        application.getUniversityName(),
                        "Mobility application",
                        application.getStatus().name(),
                        application.getCreatedAt(),
                        application.getCreatedAt(),
                        deadline(application.getCreatedAt(), 14),
                        isOverdue(deadline(application.getCreatedAt(), 14), application.getStatus().name(), List.of("APPROVED", "REJECTED")),
                        allowedMobilityTransitions(application.getStatus()).stream().map(Enum::name).toList(),
                        "/app/student"
                )));

        clearanceSheetRepository.findByStudentIdWithDetails(student.getId()).ifPresent(sheet -> {
            if (sheet.getStatus() != ClearanceSheet.ClearanceStatus.CLEARED) {
                items.add(new WorkflowItem(
                        WorkflowType.CLEARANCE,
                        sheet.getId(),
                        "Clearance sheet",
                        "Graduation / transfer workflow",
                        sheet.getStatus().name(),
                        null,
                        null,
                        null,
                        false,
                        List.of(ClearanceSheet.ClearanceStatus.CLEARED.name()),
                        "/app/student"
                ));
            }
        });

        registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(registration -> registration.getStatus() == Registration.RegistrationStatus.SUBMITTED)
                .forEach(registration -> items.add(new WorkflowItem(
                        WorkflowType.REGISTRATION,
                        registration.getId(),
                        registration.getSubjectOffering() != null && registration.getSubjectOffering().getSubject() != null
                                ? registration.getSubjectOffering().getSubject().getCode()
                                : "Registration",
                        "Course registration",
                        registration.getStatus().name(),
                        registration.getCreatedAt(),
                        registration.getCreatedAt(),
                        resolveWindowDeadline(registration.getSubjectOffering(), RegistrationWindow.WindowType.REGISTRATION, registration.getCreatedAt(), 7),
                        isOverdue(resolveWindowDeadline(registration.getSubjectOffering(), RegistrationWindow.WindowType.REGISTRATION, registration.getCreatedAt(), 7),
                                registration.getStatus().name(), List.of("CONFIRMED", "DROPPED")),
                        allowedRegistrationTransitions(registration.getStatus()).stream().map(Enum::name).toList(),
                        "/app/student/enrollments"
                )));

        return new WorkflowOverview(items.stream()
                .sorted(Comparator.comparing(WorkflowItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
    }

    @Transactional(readOnly = true)
    public WorkflowOverview buildAdminOverview() {
        List<WorkflowItem> items = new ArrayList<>();

        studentRequestRepository.findAllWithDetails().stream()
                .filter(request -> request.getStatus() != StudentRequest.RequestStatus.DONE)
                .forEach(request -> items.add(new WorkflowItem(
                        WorkflowType.REQUEST,
                        request.getId(),
                        request.getCategory() != null ? request.getCategory() : "Request",
                        request.getStudent() != null ? request.getStudent().getName() : "Student request",
                        request.getStatus().name(),
                        request.getCreatedAt(),
                        request.getUpdatedAt(),
                        deadline(request.getCreatedAt(), 3),
                        isOverdue(deadline(request.getCreatedAt(), 3), request.getStatus().name(), List.of("DONE", "REJECTED")),
                        allowedRequestTransitions(request.getStatus()).stream().map(Enum::name).toList(),
                        "/app/admin/requests"
                )));

        fxRegistrationRepository.findAllWithDetailsOrderByCreatedAtDesc().stream()
                .filter(fx -> fx.getStatus() != FxRegistration.FxStatus.CONFIRMED)
                .forEach(fx -> items.add(new WorkflowItem(
                        WorkflowType.FX,
                        fx.getId(),
                        fx.getSubjectOffering() != null && fx.getSubjectOffering().getSubject() != null
                                ? fx.getSubjectOffering().getSubject().getCode()
                                : "FX",
                        fx.getStudent() != null ? fx.getStudent().getName() : "FX registration",
                        fx.getStatus().name(),
                        fx.getCreatedAt(),
                        fx.getCreatedAt(),
                        resolveWindowDeadline(fx.getSubjectOffering(), RegistrationWindow.WindowType.FX, fx.getCreatedAt(), 7),
                        isOverdue(resolveWindowDeadline(fx.getSubjectOffering(), RegistrationWindow.WindowType.FX, fx.getCreatedAt(), 7),
                                fx.getStatus().name(), List.of("CONFIRMED")),
                        allowedFxTransitions(fx.getStatus()).stream().map(Enum::name).toList(),
                        "/app/admin/registration"
                )));

        mobilityApplicationRepository.findAllWithStudent().stream()
                .filter(application -> application.getStatus() != MobilityApplication.MobilityStatus.APPROVED
                        && application.getStatus() != MobilityApplication.MobilityStatus.REJECTED)
                .forEach(application -> items.add(new WorkflowItem(
                        WorkflowType.MOBILITY,
                        application.getId(),
                        application.getUniversityName(),
                        application.getStudent() != null ? application.getStudent().getName() : "Mobility",
                        application.getStatus().name(),
                        application.getCreatedAt(),
                        application.getCreatedAt(),
                        deadline(application.getCreatedAt(), 14),
                        isOverdue(deadline(application.getCreatedAt(), 14), application.getStatus().name(), List.of("APPROVED", "REJECTED")),
                        allowedMobilityTransitions(application.getStatus()).stream().map(Enum::name).toList(),
                        "/app/admin/academic"
                )));

        clearanceSheetRepository.findAllWithStudentAndCheckpoints().stream()
                .filter(sheet -> sheet.getStatus() != ClearanceSheet.ClearanceStatus.CLEARED)
                .forEach(sheet -> items.add(new WorkflowItem(
                        WorkflowType.CLEARANCE,
                        sheet.getId(),
                        "Clearance sheet",
                        sheet.getStudent() != null ? sheet.getStudent().getName() : "Clearance",
                        sheet.getStatus().name(),
                        null,
                        null,
                        null,
                        false,
                        List.of(ClearanceSheet.ClearanceStatus.CLEARED.name()),
                        "/app/admin/academic"
                )));

        gradeChangeRequestRepository.findByStatusWithDetailsOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus.SUBMITTED)
                .forEach(request -> items.add(new WorkflowItem(
                        WorkflowType.GRADE_CHANGE,
                        request.getId(),
                        request.getSubjectOffering() != null && request.getSubjectOffering().getSubject() != null
                                ? request.getSubjectOffering().getSubject().getCode()
                                : "Grade change",
                        request.getStudent() != null ? request.getStudent().getName() : "Grade change request",
                        request.getStatus().name(),
                        request.getCreatedAt(),
                        request.getReviewedAt(),
                        deadline(request.getCreatedAt(), 3),
                        isOverdue(deadline(request.getCreatedAt(), 3), request.getStatus().name(), List.of("REJECTED", "APPLIED")),
                        allowedGradeChangeTransitions(request.getStatus()).stream().map(Enum::name).toList(),
                        "/app/admin/moderation"
                )));

        return new WorkflowOverview(items.stream()
                .sorted(Comparator.comparing(WorkflowItem::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
    }

    @Transactional(readOnly = true)
    public WorkflowTimeline buildTimeline(WorkflowType type, Long entityId) {
        String entityType = mapAuditEntityType(type);
        List<AuditLog> logs = entityType == null
                ? List.of()
                : auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
        List<WorkflowTimelineItem> items = logs.stream()
                .map(log -> new WorkflowTimelineItem(
                        log.getCreatedAt(),
                        log.getAction(),
                        log.getActorEmail(),
                        log.getDetails()
                ))
                .toList();
        return new WorkflowTimeline(type, entityId, items);
    }

    public List<StudentRequest.RequestStatus> allowedRequestTransitions(StudentRequest.RequestStatus current) {
        return switch (current) {
            case NEW -> List.of(StudentRequest.RequestStatus.IN_REVIEW, StudentRequest.RequestStatus.NEED_INFO,
                    StudentRequest.RequestStatus.APPROVED, StudentRequest.RequestStatus.REJECTED);
            case IN_REVIEW -> List.of(StudentRequest.RequestStatus.NEED_INFO, StudentRequest.RequestStatus.APPROVED,
                    StudentRequest.RequestStatus.REJECTED, StudentRequest.RequestStatus.DONE);
            case NEED_INFO -> List.of(StudentRequest.RequestStatus.IN_REVIEW, StudentRequest.RequestStatus.APPROVED,
                    StudentRequest.RequestStatus.REJECTED);
            case APPROVED, REJECTED -> List.of(StudentRequest.RequestStatus.DONE);
            case DONE -> List.of();
        };
    }

    public List<FxRegistration.FxStatus> allowedFxTransitions(FxRegistration.FxStatus current) {
        return switch (current) {
            case PENDING -> List.of(FxRegistration.FxStatus.APPROVED);
            case APPROVED -> List.of(FxRegistration.FxStatus.PAID, FxRegistration.FxStatus.CONFIRMED);
            case PAID -> List.of(FxRegistration.FxStatus.CONFIRMED);
            case CONFIRMED -> List.of();
        };
    }

    public List<MobilityApplication.MobilityStatus> allowedMobilityTransitions(MobilityApplication.MobilityStatus current) {
        return switch (current) {
            case DRAFT -> List.of(MobilityApplication.MobilityStatus.SUBMITTED);
            case SUBMITTED -> List.of(MobilityApplication.MobilityStatus.IN_REVIEW, MobilityApplication.MobilityStatus.REJECTED);
            case IN_REVIEW -> List.of(MobilityApplication.MobilityStatus.APPROVED, MobilityApplication.MobilityStatus.REJECTED);
            case APPROVED, REJECTED -> List.of();
        };
    }

    public List<GradeChangeRequest.RequestStatus> allowedGradeChangeTransitions(GradeChangeRequest.RequestStatus current) {
        return switch (current) {
            case SUBMITTED -> List.of(GradeChangeRequest.RequestStatus.APPROVED, GradeChangeRequest.RequestStatus.REJECTED);
            case APPROVED -> List.of(GradeChangeRequest.RequestStatus.APPLIED);
            case REJECTED, APPLIED -> List.of();
        };
    }

    public List<Registration.RegistrationStatus> allowedRegistrationTransitions(Registration.RegistrationStatus current) {
        return switch (current) {
            case DRAFT -> List.of(Registration.RegistrationStatus.SUBMITTED);
            case SUBMITTED -> List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.DROPPED);
            case CONFIRMED -> List.of(Registration.RegistrationStatus.DROPPED);
            case DROPPED -> List.of(Registration.RegistrationStatus.SUBMITTED, Registration.RegistrationStatus.CONFIRMED);
        };
    }

    public List<ClearanceCheckpoint.CheckpointStatus> allowedClearanceCheckpointTransitions(ClearanceCheckpoint.CheckpointStatus current) {
        return switch (current) {
            case PENDING -> List.of(ClearanceCheckpoint.CheckpointStatus.APPROVED, ClearanceCheckpoint.CheckpointStatus.REJECTED);
            case REJECTED -> List.of(ClearanceCheckpoint.CheckpointStatus.APPROVED);
            case APPROVED -> List.of();
        };
    }

    private Instant deadline(Instant createdAt, int days) {
        return createdAt != null ? createdAt.plusSeconds(days * 24L * 60L * 60L) : null;
    }

    private boolean isOverdue(Instant deadline, String status, List<String> terminalStates) {
        return deadline != null && !terminalStates.contains(status) && deadline.isBefore(Instant.now());
    }

    private Instant resolveWindowDeadline(SubjectOffering offering, RegistrationWindow.WindowType windowType, Instant fallbackStart, int fallbackDays) {
        if (offering != null && offering.getSemester() != null) {
            return registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(offering.getSemester().getId(), windowType)
                    .map(window -> window.getEndDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                    .orElse(deadline(fallbackStart, fallbackDays));
        }
        return deadline(fallbackStart, fallbackDays);
    }

    private String mapAuditEntityType(WorkflowType type) {
        return switch (type) {
            case REQUEST -> "StudentRequest";
            case FX -> "FxRegistration";
            case MOBILITY -> "MobilityApplication";
            case CLEARANCE -> "ClearanceSheet";
            case GRADE_CHANGE -> "GradeChangeRequest";
            case REGISTRATION -> "Registration";
        };
    }

    public enum WorkflowType {
        REQUEST,
        FX,
        MOBILITY,
        CLEARANCE,
        GRADE_CHANGE,
        REGISTRATION
    }

    public record WorkflowItem(
            WorkflowType type,
            Long entityId,
            String title,
            String subject,
            String status,
            Instant createdAt,
            Instant updatedAt,
            Instant dueAt,
            boolean overdue,
            List<String> nextStatuses,
            String link
    ) {
    }

    public record WorkflowOverview(List<WorkflowItem> items) {
    }

    public record WorkflowTimeline(WorkflowType type, Long entityId, List<WorkflowTimelineItem> items) {
    }

    public record WorkflowTimelineItem(Instant createdAt, String action, String actorEmail, String details) {
    }
}
