package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.ExamSchedule;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.ExamScheduleRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamScheduleService {

    private final ExamScheduleRepository examScheduleRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final AuditService auditService;

    public List<ExamSchedule> listBySemester(Long semesterId) {
        return examScheduleRepository.findBySubjectOffering_SemesterIdOrderByExamDateAsc(semesterId);
    }

    @Transactional
    public ExamSchedule createExamSession(Long sectionId, LocalDate examDate, LocalTime examTime,
                                           String room, String format, User actor) {
        SubjectOffering section = subjectOfferingRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        checkExamConflict(section.getSemester().getId(), examDate, examTime, room, null);

        ExamSchedule exam = ExamSchedule.builder()
                .subjectOffering(section)
                .examDate(examDate)
                .examTime(examTime)
                .room(room)
                .format(format)
                .build();
        ExamSchedule saved = examScheduleRepository.save(exam);
        auditService.logUserAction(actor, "EXAM_SCHEDULED", "ExamSchedule", saved.getId(),
                "sectionId=" + sectionId + ", date=" + examDate + ", room=" + room);
        return saved;
    }

    @Transactional
    public ExamSchedule updateExamSession(Long examId, LocalDate examDate, LocalTime examTime,
                                           String room, String format, User actor) {
        ExamSchedule exam = examScheduleRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));

        checkExamConflict(exam.getSubjectOffering().getSemester().getId(), examDate, examTime, room, examId);

        exam.setExamDate(examDate);
        exam.setExamTime(examTime);
        exam.setRoom(room);
        exam.setFormat(format);
        ExamSchedule saved = examScheduleRepository.save(exam);
        auditService.logUserAction(actor, "EXAM_UPDATED", "ExamSchedule", saved.getId(),
                "date=" + examDate + ", room=" + room);
        return saved;
    }

    @Transactional
    public void deleteExamSession(Long examId, User actor) {
        ExamSchedule exam = examScheduleRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam session not found"));
        auditService.logUserAction(actor, "EXAM_DELETED", "ExamSchedule", examId,
                "sectionId=" + exam.getSubjectOffering().getId());
        examScheduleRepository.delete(exam);
    }

    private void checkExamConflict(Long semesterId, LocalDate examDate, LocalTime examTime,
                                    String room, Long excludeExamId) {
        if (room == null || room.isBlank()) {
            return;
        }
        LocalTime examEnd = examTime.plusHours(2);
        List<ExamSchedule> exams = examScheduleRepository.findBySubjectOffering_SemesterIdOrderByExamDateAsc(semesterId);
        for (ExamSchedule existing : exams) {
            if (excludeExamId != null && existing.getId().equals(excludeExamId)) {
                continue;
            }
            if (existing.getExamDate().equals(examDate)
                    && existing.getRoom() != null
                    && existing.getRoom().equalsIgnoreCase(room)) {
                LocalTime existingEnd = existing.getExamTime().plusHours(2);
                if (existing.getExamTime().isBefore(examEnd) && existingEnd.isAfter(examTime)) {
                    throw new IllegalStateException("Exam room conflict: " + room + " on " + examDate
                            + " at " + existing.getExamTime());
                }
            }
        }
    }
}
