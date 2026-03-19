package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.Attendance;
import ru.kors.finalproject.entity.AttendanceSession;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.Registration;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.repository.AttendanceRepository;
import ru.kors.finalproject.repository.AttendanceSessionRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AttendanceFlowService {

    private static final List<Registration.RegistrationStatus> ACTIVE_REGISTRATION_STATUSES = List.of(
            Registration.RegistrationStatus.CONFIRMED,
            Registration.RegistrationStatus.SUBMITTED
    );

    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public AttendanceSession openSession(
            Teacher teacher,
            Long offeringId,
            LocalDate classDate,
            Instant closeAt,
            AttendanceSession.CheckInMode checkInMode,
            String requestedCode,
            boolean allowTeacherOverride
    ) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        AttendanceSession session = attendanceSessionRepository.findBySubjectOfferingIdAndClassDate(offeringId, classDate)
                .orElseGet(() -> AttendanceSession.builder()
                        .subjectOffering(offering)
                        .classDate(classDate)
                        .createdBy(teacher)
                        .createdAt(Instant.now())
                        .locked(false)
                        .build());

        session.setStatus(AttendanceSession.SessionStatus.OPEN);
        session.setLocked(false);
        session.setCheckInMode(checkInMode);
        session.setAllowTeacherOverride(allowTeacherOverride);
        session.setAttendanceCloseAt(closeAt);
        session.setOpenedAt(Instant.now());
        session.setClosedAt(null);
        session.setCheckInCode(checkInMode == AttendanceSession.CheckInMode.CODE
                ? normalizeCheckInCode(requestedCode)
                : null);
        AttendanceSession saved = attendanceSessionRepository.save(session);

        publishSectionEvent(saved, "ATTENDANCE_OPENED", null, null, null);
        publishStudentEvents(saved, "ATTENDANCE_OPENED");
        auditService.logUserAction(
                null,
                "ATTENDANCE_SESSION_OPENED",
                "AttendanceSession",
                saved.getId(),
                "offeringId=" + offeringId + ", classDate=" + classDate + ", closeAt=" + closeAt + ", mode=" + checkInMode
        );
        return saved;
    }

    @Transactional
    public AttendanceSession closeSession(Teacher teacher, Long sessionId) {
        AttendanceSession session = getTeacherSession(teacher, sessionId);
        if (session.getStatus() == AttendanceSession.SessionStatus.CLOSED && session.isLocked()) {
            return session;
        }

        List<Registration> roster = getActiveRoster(session.getSubjectOffering().getId());
        Map<Long, Attendance> existingByStudentId = new LinkedHashMap<>();
        attendanceRepository.findBySessionIdWithDetails(sessionId)
                .forEach(attendance -> existingByStudentId.put(attendance.getStudent().getId(), attendance));

        List<Attendance> generatedAbsences = new ArrayList<>();
        Instant now = Instant.now();
        for (Registration registration : roster) {
            Student student = registration.getStudent();
            if (student == null || existingByStudentId.containsKey(student.getId())) {
                continue;
            }
            Attendance absent = Attendance.builder()
                    .student(student)
                    .subjectOffering(session.getSubjectOffering())
                    .session(session)
                    .date(session.getClassDate())
                    .status(Attendance.AttendanceStatus.ABSENT)
                    .reason("Session closed without check-in")
                    .markedBy(Attendance.MarkedBy.SYSTEM)
                    .teacherConfirmed(true)
                    .markedAt(now)
                    .updatedAt(now)
                    .build();
            generatedAbsences.add(absent);
        }
        if (!generatedAbsences.isEmpty()) {
            attendanceRepository.saveAll(generatedAbsences);
            generatedAbsences.forEach(absent -> notificationService.notifyStudent(
                    absent.getStudent().getEmail(),
                    Notification.NotificationType.ATTENDANCE,
                    "Attendance closed",
                    session.getSubjectOffering().getSubject().getCode() + " attendance closed with ABSENT status",
                    "/app/student/attendance"
            ));
        }

        session.setStatus(AttendanceSession.SessionStatus.CLOSED);
        session.setLocked(true);
        session.setClosedAt(now);
        AttendanceSession saved = attendanceSessionRepository.save(session);
        publishSectionEvent(saved, "ATTENDANCE_CLOSED", null, null, null);
        publishStudentEvents(saved, "ATTENDANCE_CLOSED");
        auditService.logUserAction(
                null,
                "ATTENDANCE_SESSION_CLOSED",
                "AttendanceSession",
                saved.getId(),
                "offeringId=" + saved.getSubjectOffering().getId() + ", generatedAbsences=" + generatedAbsences.size()
        );
        return saved;
    }

    @Transactional
    public Attendance checkIn(Student student, Long sessionId, String code) {
        AttendanceSession session = getStudentSession(student, sessionId);
        validateOpenSession(session);
        if (session.getCheckInMode() == AttendanceSession.CheckInMode.CODE) {
            String normalizedCode = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
            if (normalizedCode.isBlank() || !normalizedCode.equals(session.getCheckInCode())) {
                throw new IllegalArgumentException("Invalid attendance code");
            }
        }

        Attendance existing = attendanceRepository.findByStudentIdAndSubjectOfferingIdAndDateWithDetails(
                student.getId(),
                session.getSubjectOffering().getId(),
                session.getClassDate()
        ).orElse(null);
        if (existing != null && existing.getMarkedBy() == Attendance.MarkedBy.TEACHER && existing.isTeacherConfirmed()) {
            throw new IllegalStateException("Attendance already finalized by teacher");
        }

        Instant now = Instant.now();
        Attendance attendance = existing != null ? existing : Attendance.builder()
                .student(student)
                .subjectOffering(session.getSubjectOffering())
                .session(session)
                .date(session.getClassDate())
                .build();

        attendance.setSession(session);
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        attendance.setReason(null);
        attendance.setMarkedBy(Attendance.MarkedBy.STUDENT);
        attendance.setTeacherConfirmed(false);
        attendance.setMarkedAt(existing != null && existing.getMarkedAt() != null ? existing.getMarkedAt() : now);
        attendance.setUpdatedAt(now);

        Attendance saved = attendanceRepository.save(attendance);
        publishSectionEvent(session, "ATTENDANCE_CHECKED_IN", student.getId(), saved.getStatus(), saved.getUpdatedAt());
        publishStudentEvent(student.getEmail(), session, "ATTENDANCE_SELF_CHECKED_IN", saved.getStatus(), saved.getUpdatedAt());
        auditService.logStudentAction(
                null,
                "ATTENDANCE_SELF_CHECK_IN",
                "Attendance",
                saved.getId(),
                "sessionId=" + sessionId + ", studentId=" + student.getId() + ", offeringId=" + session.getSubjectOffering().getId()
        );
        return saved;
    }

    @Transactional
    public Attendance overrideAttendance(
            Teacher teacher,
            Long sessionId,
            Long studentId,
            Attendance.AttendanceStatus status,
            String reason
    ) {
        AttendanceSession session = getTeacherSession(teacher, sessionId);
        if (!session.isAllowTeacherOverride()) {
            throw new IllegalStateException("Teacher override is disabled for this attendance session");
        }

        Registration registration = registrationRepository.findByStudentIdAndSubjectOfferingIdWithDetails(
                        studentId, session.getSubjectOffering().getId())
                .filter(item -> ACTIVE_REGISTRATION_STATUSES.contains(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Student is not enrolled in this section"));

        Attendance attendance = attendanceRepository.findByStudentIdAndSubjectOfferingIdAndDateWithDetails(
                studentId,
                session.getSubjectOffering().getId(),
                session.getClassDate()
        ).orElseGet(() -> Attendance.builder()
                .student(registration.getStudent())
                .subjectOffering(session.getSubjectOffering())
                .session(session)
                .date(session.getClassDate())
                .build());

        Instant now = Instant.now();
        attendance.setSession(session);
        attendance.setStatus(status);
        attendance.setReason(reason);
        attendance.setMarkedBy(Attendance.MarkedBy.TEACHER);
        attendance.setTeacherConfirmed(true);
        attendance.setMarkedAt(attendance.getMarkedAt() != null ? attendance.getMarkedAt() : now);
        attendance.setUpdatedAt(now);
        Attendance saved = attendanceRepository.save(attendance);

        notificationService.notifyStudent(
                registration.getStudent().getEmail(),
                Notification.NotificationType.ATTENDANCE,
                "Attendance updated by teacher",
                session.getSubjectOffering().getSubject().getCode() + " attendance is now " + status,
                "/app/student/attendance"
        );
        publishSectionEvent(session, "ATTENDANCE_OVERRIDDEN", studentId, status, saved.getUpdatedAt());
        publishStudentEvent(registration.getStudent().getEmail(), session, "ATTENDANCE_OVERRIDDEN", status, saved.getUpdatedAt());
        auditService.logUserAction(
                null,
                "ATTENDANCE_OVERRIDDEN",
                "Attendance",
                saved.getId(),
                "sessionId=" + sessionId + ", studentId=" + studentId + ", status=" + status
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public AttendanceSession getSectionSession(Teacher teacher, Long offeringId, LocalDate classDate) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        return attendanceSessionRepository.findBySubjectOfferingIdAndClassDate(offering.getId(), classDate).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TeacherAttendanceRecordView> getSessionRosterView(Teacher teacher, Long sessionId) {
        AttendanceSession session = getTeacherSession(teacher, sessionId);
        List<Registration> roster = getActiveRoster(session.getSubjectOffering().getId());
        Map<Long, Attendance> attendanceByStudentId = new LinkedHashMap<>();
        attendanceRepository.findBySessionIdWithDetails(sessionId)
                .forEach(attendance -> attendanceByStudentId.put(attendance.getStudent().getId(), attendance));

        return roster.stream()
                .map(registration -> {
                    Student student = registration.getStudent();
                    Attendance attendance = attendanceByStudentId.get(student.getId());
                    return new TeacherAttendanceRecordView(
                            student.getId(),
                            student.getName(),
                            student.getEmail(),
                            attendance != null ? attendance.getId() : null,
                            attendance != null ? attendance.getStatus() : null,
                            attendance != null ? attendance.getReason() : null,
                            attendance != null ? attendance.getMarkedBy() : null,
                            attendance != null && attendance.isTeacherConfirmed(),
                            attendance != null ? attendance.getMarkedAt() : null,
                            attendance != null ? attendance.getUpdatedAt() : null
                    );
                })
                .sorted(Comparator.comparing(TeacherAttendanceRecordView::studentName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentActiveAttendanceSessionView> getActiveSessionsForStudent(Student student) {
        List<Registration> registrations = registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .filter(registration -> ACTIVE_REGISTRATION_STATUSES.contains(registration.getStatus()))
                .toList();
        if (registrations.isEmpty()) {
            return List.of();
        }

        List<Long> offeringIds = registrations.stream()
                .map(registration -> registration.getSubjectOffering().getId())
                .distinct()
                .toList();
        Map<Long, Registration> registrationByOfferingId = registrations.stream()
                .collect(LinkedHashMap::new, (map, registration) -> map.put(registration.getSubjectOffering().getId(), registration), Map::putAll);

        Instant now = Instant.now();
        return attendanceSessionRepository.findBySubjectOfferingIdInAndStatusWithDetails(offeringIds, AttendanceSession.SessionStatus.OPEN).stream()
                .filter(session -> !session.isLocked())
                .filter(session -> session.getAttendanceCloseAt() == null || session.getAttendanceCloseAt().isAfter(now))
                .map(session -> {
                    Registration registration = registrationByOfferingId.get(session.getSubjectOffering().getId());
                    Attendance attendance = attendanceRepository.findByStudentIdAndSubjectOfferingIdAndDateWithDetails(
                            student.getId(),
                            session.getSubjectOffering().getId(),
                            session.getClassDate()
                    ).orElse(null);
                    return new StudentActiveAttendanceSessionView(
                            session.getId(),
                            session.getSubjectOffering().getId(),
                            session.getSubjectOffering().getSubject().getCode(),
                            session.getSubjectOffering().getSubject().getName(),
                            session.getSubjectOffering().getTeacher() != null ? session.getSubjectOffering().getTeacher().getName() : null,
                            session.getClassDate(),
                            session.getAttendanceCloseAt(),
                            session.getCheckInMode(),
                            attendance != null ? attendance.getStatus() : null,
                            attendance != null ? attendance.getMarkedBy() : null,
                            attendance != null && attendance.isTeacherConfirmed(),
                            registration != null ? registration.getStatus() : null
                    );
                })
                .sorted(Comparator.comparing(StudentActiveAttendanceSessionView::attendanceCloseAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void validateOpenSession(AttendanceSession session) {
        if (session.getStatus() != AttendanceSession.SessionStatus.OPEN || session.isLocked()) {
            throw new IllegalStateException("Attendance session is not open");
        }
        if (session.getAttendanceCloseAt() != null && !session.getAttendanceCloseAt().isAfter(Instant.now())) {
            throw new IllegalStateException("Attendance session is already closed");
        }
    }

    private AttendanceSession getTeacherSession(Teacher teacher, Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance session not found"));
        if (session.getSubjectOffering() == null
                || session.getSubjectOffering().getTeacher() == null
                || !Objects.equals(session.getSubjectOffering().getTeacher().getId(), teacher.getId())) {
            throw new IllegalArgumentException("Attendance session does not belong to current teacher");
        }
        return session;
    }

    private AttendanceSession getStudentSession(Student student, Long sessionId) {
        AttendanceSession session = attendanceSessionRepository.findByIdWithDetails(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance session not found"));
        boolean enrolled = registrationRepository.findByStudentIdAndSubjectOfferingId(
                        student.getId(),
                        session.getSubjectOffering().getId())
                .filter(registration -> ACTIVE_REGISTRATION_STATUSES.contains(registration.getStatus()))
                .isPresent();
        if (!enrolled) {
            throw new IllegalArgumentException("Student is not enrolled in this section");
        }
        return session;
    }

    private SubjectOffering getTeacherOffering(Teacher teacher, Long offeringId) {
        SubjectOffering offering = subjectOfferingRepository.findByIdWithDetails(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (offering.getTeacher() == null || !Objects.equals(offering.getTeacher().getId(), teacher.getId())) {
            throw new IllegalArgumentException("Section is not assigned to current teacher");
        }
        return offering;
    }

    private List<Registration> getActiveRoster(Long offeringId) {
        return registrationRepository.findBySubjectOfferingIdAndStatusInWithDetails(offeringId, ACTIVE_REGISTRATION_STATUSES);
    }

    private String normalizeCheckInCode(String requestedCode) {
        String normalized = requestedCode == null ? "" : requestedCode.trim().toUpperCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return normalized;
        }
        return randomCode();
    }

    private String randomCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder builder = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < 6; index++) {
            builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private void publishSectionEvent(
            AttendanceSession session,
            String eventType,
            Long studentId,
            Attendance.AttendanceStatus status,
            Instant changedAt
    ) {
        messagingTemplate.convertAndSend(
                "/topic/attendance/section/" + session.getSubjectOffering().getId(),
                new AttendanceRealtimeEvent(
                        eventType,
                        session.getId(),
                        session.getSubjectOffering().getId(),
                        studentId,
                        status != null ? status.name() : null,
                        session.getAttendanceCloseAt(),
                        changedAt != null ? changedAt : Instant.now()
                )
        );
    }

    private void publishStudentEvents(AttendanceSession session, String eventType) {
        getActiveRoster(session.getSubjectOffering().getId()).forEach(registration ->
                publishStudentEvent(registration.getStudent().getEmail(), session, eventType, null, Instant.now()));
    }

    private void publishStudentEvent(
            String studentEmail,
            AttendanceSession session,
            String eventType,
            Attendance.AttendanceStatus status,
            Instant changedAt
    ) {
        messagingTemplate.convertAndSendToUser(
                studentEmail,
                "/queue/attendance",
                new AttendanceRealtimeEvent(
                        eventType,
                        session.getId(),
                        session.getSubjectOffering().getId(),
                        null,
                        status != null ? status.name() : null,
                        session.getAttendanceCloseAt(),
                        changedAt != null ? changedAt : Instant.now()
                )
        );
    }

    public record TeacherAttendanceRecordView(
            Long studentId,
            String studentName,
            String studentEmail,
            Long attendanceId,
            Attendance.AttendanceStatus status,
            String reason,
            Attendance.MarkedBy markedBy,
            boolean teacherConfirmed,
            Instant markedAt,
            Instant updatedAt
    ) {
    }

    public record StudentActiveAttendanceSessionView(
            Long sessionId,
            Long sectionId,
            String subjectCode,
            String subjectName,
            String teacherName,
            LocalDate classDate,
            Instant attendanceCloseAt,
            AttendanceSession.CheckInMode checkInMode,
            Attendance.AttendanceStatus currentStatus,
            Attendance.MarkedBy markedBy,
            boolean teacherConfirmed,
            Registration.RegistrationStatus registrationStatus
    ) {
    }

    public record AttendanceRealtimeEvent(
            String eventType,
            Long sessionId,
            Long sectionId,
            Long studentId,
            String status,
            Instant attendanceCloseAt,
            Instant timestamp
    ) {
    }

    public Instant parseCloseAt(String value) {
        if (value == null || value.isBlank()) {
            return Instant.now().plusSeconds(15 * 60);
        }
        try {
            return Instant.parse(value);
        } catch (Exception ignored) {
            return LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant();
        }
    }
}
