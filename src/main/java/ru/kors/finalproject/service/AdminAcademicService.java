package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAcademicService {

    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final TeacherRepository teacherRepository;
    private final MeetingTimeRepository meetingTimeRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final RegistrationRepository registrationRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AddDropService addDropService;

    @Transactional
    public Semester createTerm(String name, LocalDate startDate, LocalDate endDate, boolean current, User actor) {
        if (current) {
            semesterRepository.findByCurrentTrue().ifPresent(s -> {
                s.setCurrent(false);
                semesterRepository.save(s);
            });
        }
        Semester semester = Semester.builder()
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .current(current)
                .build();
        Semester saved = semesterRepository.save(semester);
        auditService.logUserAction(actor, "TERM_CREATED", "Semester", saved.getId(), "name=" + name);
        return saved;
    }

    @Transactional
    public Semester updateTerm(Long termId, String name, LocalDate startDate, LocalDate endDate, boolean current, User actor) {
        Semester semester = semesterRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Term not found"));
        if (current && !semester.isCurrent()) {
            semesterRepository.findByCurrentTrue().ifPresent(s -> {
                s.setCurrent(false);
                semesterRepository.save(s);
            });
        }
        semester.setName(name);
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setCurrent(current);
        Semester saved = semesterRepository.save(semester);
        auditService.logUserAction(actor, "TERM_UPDATED", "Semester", saved.getId(), "name=" + name);
        return saved;
    }

    public List<Semester> listTerms() {
        return semesterRepository.findAll();
    }

    @Transactional
    public Subject createSubject(String code, String name, int credits, Long programId, User actor) {
        Program programRef = null;
        if (programId != null) {
            programRef = new Program();
            programRef.setId(programId);
        }
        Subject subject = Subject.builder()
                .code(code)
                .name(name)
                .credits(credits)
                .program(programRef)
                .build();
        Subject saved = subjectRepository.save(subject);
        auditService.logUserAction(actor, "SUBJECT_CREATED", "Subject", saved.getId(), "code=" + code);
        return saved;
    }

    @Transactional
    public SubjectPrerequisite addPrerequisite(Long subjectId, Long prerequisiteId, User actor) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        Subject prerequisite = subjectRepository.findById(prerequisiteId)
                .orElseThrow(() -> new IllegalArgumentException("Prerequisite subject not found"));
        if (subjectId.equals(prerequisiteId)) {
            throw new IllegalArgumentException("Subject cannot be its own prerequisite");
        }
        SubjectPrerequisite sp = SubjectPrerequisite.builder()
                .subject(subject)
                .prerequisite(prerequisite)
                .build();
        SubjectPrerequisite saved = subjectPrerequisiteRepository.save(sp);
        auditService.logUserAction(actor, "PREREQUISITE_ADDED", "SubjectPrerequisite", saved.getId(),
                "subjectId=" + subjectId + ", prerequisiteId=" + prerequisiteId);
        return saved;
    }

    @Transactional
    public SubjectOffering createSection(Long subjectId, Long semesterId, Long teacherId,
                                         int capacity, SubjectOffering.LessonType lessonType, User actor) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        Teacher teacher = teacherId != null
                ? teacherRepository.findById(teacherId).orElseThrow(() -> new IllegalArgumentException("Teacher not found"))
                : null;

        SubjectOffering section = SubjectOffering.builder()
                .subject(subject)
                .semester(semester)
                .teacher(teacher)
                .capacity(capacity)
                .lessonType(lessonType)
                .build();
        SubjectOffering saved = subjectOfferingRepository.save(section);
        auditService.logUserAction(actor, "SECTION_CREATED", "SubjectOffering", saved.getId(),
                "subjectCode=" + subject.getCode() + ", semesterName=" + semester.getName());
        if (teacher != null) {
            notificationService.notifyStudent(
                    teacher.getEmail(),
                    Notification.NotificationType.SYSTEM,
                    "New section assigned",
                    "You were assigned to teach " + subject.getCode() + " in " + semester.getName(),
                    "/app/teacher/sections"
            );
        }
        return saved;
    }

    @Transactional
    public SubjectOffering assignProfessor(Long sectionId, Long teacherId, User actor) {
        SubjectOffering section = subjectOfferingRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        section.setTeacher(teacher);
        SubjectOffering saved = subjectOfferingRepository.save(section);
        auditService.logUserAction(actor, "PROFESSOR_ASSIGNED", "SubjectOffering", saved.getId(),
                "teacherId=" + teacherId);
        notificationService.notifyStudent(
                teacher.getEmail(),
                Notification.NotificationType.SYSTEM,
                "Section assignment updated",
                "You were assigned to section #" + saved.getId() + " for "
                        + saved.getSubject().getCode() + " " + saved.getSubject().getName(),
                "/app/teacher/sections"
        );
        return saved;
    }

    @Transactional
    public MeetingTime addMeetingTime(Long sectionId, DayOfWeek dayOfWeek, LocalTime startTime,
                                       LocalTime endTime, String room,
                                       SubjectOffering.LessonType lessonType, User actor) {
        SubjectOffering section = subjectOfferingRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        checkRoomConflict(section.getSemester().getId(), dayOfWeek, startTime, endTime, room, null);

        if (section.getTeacher() != null) {
            checkTeacherConflict(section.getSemester().getId(), section.getTeacher().getId(),
                    dayOfWeek, startTime, endTime, null);
        }

        MeetingTime mt = MeetingTime.builder()
                .subjectOffering(section)
                .dayOfWeek(dayOfWeek)
                .startTime(startTime)
                .endTime(endTime)
                .room(room)
                .lessonType(lessonType)
                .build();
        MeetingTime saved = meetingTimeRepository.save(mt);
        auditService.logUserAction(actor, "MEETING_TIME_ADDED", "MeetingTime", saved.getId(),
                "sectionId=" + sectionId + ", day=" + dayOfWeek + ", room=" + room);
        return saved;
    }

    public void checkRoomConflict(Long semesterId, DayOfWeek dayOfWeek, LocalTime startTime,
                                   LocalTime endTime, String room, Long excludeMeetingTimeId) {
        List<SubjectOffering> semesterSections = subjectOfferingRepository.findBySemesterId(semesterId);
        for (SubjectOffering so : semesterSections) {
            for (MeetingTime mt : meetingTimeRepository.findBySubjectOfferingId(so.getId())) {
                if (excludeMeetingTimeId != null && mt.getId().equals(excludeMeetingTimeId)) {
                    continue;
                }
                if (mt.getDayOfWeek() == dayOfWeek
                        && mt.getRoom() != null && mt.getRoom().equalsIgnoreCase(room)
                        && mt.getStartTime().isBefore(endTime)
                        && mt.getEndTime().isAfter(startTime)) {
                    throw new IllegalStateException("Room conflict: " + room + " is occupied on "
                            + dayOfWeek + " " + mt.getStartTime() + "-" + mt.getEndTime()
                            + " by section #" + so.getId());
                }
            }
        }
    }

    public void checkTeacherConflict(Long semesterId, Long teacherId, DayOfWeek dayOfWeek,
                                      LocalTime startTime, LocalTime endTime, Long excludeMeetingTimeId) {
        List<SubjectOffering> teacherSections = subjectOfferingRepository.findByTeacherId(teacherId).stream()
                .filter(s -> s.getSemester().getId().equals(semesterId))
                .toList();
        for (SubjectOffering so : teacherSections) {
            for (MeetingTime mt : meetingTimeRepository.findBySubjectOfferingId(so.getId())) {
                if (excludeMeetingTimeId != null && mt.getId().equals(excludeMeetingTimeId)) {
                    continue;
                }
                if (mt.getDayOfWeek() == dayOfWeek
                        && mt.getStartTime().isBefore(endTime)
                        && mt.getEndTime().isAfter(startTime)) {
                    throw new IllegalStateException("Teacher schedule conflict on "
                            + dayOfWeek + " " + mt.getStartTime() + "-" + mt.getEndTime()
                            + " in section #" + so.getId());
                }
            }
        }
    }

    @Transactional
    public RegistrationWindow upsertWindow(Long semesterId, RegistrationWindow.WindowType type,
                                            LocalDate startDate, LocalDate endDate, boolean active, User actor) {
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester not found"));
        RegistrationWindow window = registrationWindowRepository
                .findBySemesterIdAndTypeAndActiveTrue(semesterId, type)
                .orElseGet(() -> RegistrationWindow.builder().semester(semester).type(type).build());
        window.setStartDate(startDate);
        window.setEndDate(endDate);
        window.setActive(active);
        RegistrationWindow saved = registrationWindowRepository.save(window);
        auditService.logUserAction(actor, "WINDOW_UPSERTED", "RegistrationWindow", saved.getId(),
                "type=" + type + ", active=" + active);
        if (active) {
            List<String> recipientEmails = studentRepository.findAllWithDetails().stream()
                    .filter(student -> student.getCurrentSemester() != null
                            && student.getCurrentSemester().getId().equals(semesterId))
                    .map(Student::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .distinct()
                    .toList();
            notificationService.notifyMany(
                    recipientEmails,
                    Notification.NotificationType.ENROLLMENT,
                    type + " window updated",
                    type + " is active for " + semester.getName() + " from " + startDate + " to " + endDate,
                    "/app/student/registration"
            );
        }
        return saved;
    }

    @Transactional
    public AddDropService.AddDropResult adminOverrideEnroll(Long studentId, Long sectionId, String reason, User actor) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        AddDropService.AddDropResult result = addDropService.adminOverrideEnroll(student, sectionId);
        auditService.logUserAction(actor, "ENROLLMENT_OVERRIDE", "SubjectOffering", sectionId,
                "studentId=" + studentId + ", sectionId=" + sectionId + ", reason=" + reason
                        + ", success=" + result.success());
        return result;
    }

    public List<SubjectOffering> listSections(Long semesterId) {
        if (semesterId != null) {
            return subjectOfferingRepository.findBySemesterIdWithDetails(semesterId);
        }
        return subjectOfferingRepository.findAllWithDetails();
    }

    public List<Subject> listSubjects() {
        return subjectRepository.findAll();
    }

    public List<Teacher> listTeachers() {
        return teacherRepository.findAllByOrderByNameAsc();
    }
}
