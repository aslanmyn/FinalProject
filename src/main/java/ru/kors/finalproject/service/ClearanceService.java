package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.ClearanceCheckpointRepository;
import ru.kors.finalproject.repository.ClearanceSheetRepository;
import ru.kors.finalproject.repository.StudentRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClearanceService {

    private final ClearanceSheetRepository clearanceSheetRepository;
    private final ClearanceCheckpointRepository clearanceCheckpointRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public List<ClearanceSheet> listAll() {
        return clearanceSheetRepository.findAll();
    }

    public List<ClearanceSheet> listByStatus(ClearanceSheet.ClearanceStatus status) {
        return clearanceSheetRepository.findAll().stream()
                .filter(s -> s.getStatus() == status)
                .toList();
    }

    @Transactional
    public ClearanceSheet createSheet(Long studentId, List<String> departments, User actor) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        ClearanceSheet sheet = ClearanceSheet.builder()
                .student(student)
                .status(ClearanceSheet.ClearanceStatus.IN_PROGRESS)
                .build();
        ClearanceSheet saved = clearanceSheetRepository.save(sheet);

        for (String department : departments) {
            ClearanceCheckpoint checkpoint = ClearanceCheckpoint.builder()
                    .clearanceSheet(saved)
                    .department(department)
                    .status(ClearanceCheckpoint.CheckpointStatus.PENDING)
                    .build();
            clearanceCheckpointRepository.save(checkpoint);
        }

        auditService.logUserAction(actor, "CLEARANCE_SHEET_CREATED", "ClearanceSheet", saved.getId(),
                "studentId=" + studentId + ", departments=" + departments.size());
        return saved;
    }

    @Transactional
    public ClearanceCheckpoint reviewCheckpoint(Long checkpointId, boolean approve, String comment, User actor) {
        ClearanceCheckpoint checkpoint = clearanceCheckpointRepository.findById(checkpointId)
                .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found"));

        checkpoint.setStatus(approve
                ? ClearanceCheckpoint.CheckpointStatus.APPROVED
                : ClearanceCheckpoint.CheckpointStatus.REJECTED);
        checkpoint.setComment(comment);
        ClearanceCheckpoint saved = clearanceCheckpointRepository.save(checkpoint);

        ClearanceSheet sheet = checkpoint.getClearanceSheet();
        List<ClearanceCheckpoint> allCheckpoints = clearanceCheckpointRepository
                .findByClearanceSheetId(sheet.getId());
        boolean allApproved = allCheckpoints.stream()
                .allMatch(cp -> cp.getStatus() == ClearanceCheckpoint.CheckpointStatus.APPROVED);
        if (allApproved) {
            sheet.setStatus(ClearanceSheet.ClearanceStatus.CLEARED);
            clearanceSheetRepository.save(sheet);
            notificationService.notifyStudent(
                    sheet.getStudent().getEmail(),
                    Notification.NotificationType.CLEARANCE,
                    "Clearance completed",
                    "All departments have approved your clearance",
                    "/portal/clearance-sheet"
            );
        }

        auditService.logUserAction(actor, approve ? "CLEARANCE_APPROVED" : "CLEARANCE_REJECTED",
                "ClearanceCheckpoint", saved.getId(),
                "department=" + checkpoint.getDepartment() + ", sheetId=" + sheet.getId());
        return saved;
    }
}
