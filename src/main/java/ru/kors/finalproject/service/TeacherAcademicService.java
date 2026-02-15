package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAcademicService {
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final TeacherStudentNoteRepository teacherStudentNoteRepository;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
    private final WindowPolicyService windowPolicyService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public List<SubjectOffering> getMySections(Teacher teacher) {
        return subjectOfferingRepository.findByTeacherId(teacher.getId());
    }

    public List<Registration> getRoster(Teacher teacher, Long offeringId) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        return registrationRepository.findBySubjectOfferingIdAndStatusIn(
                offering.getId(),
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
        );
    }

    @Transactional
    public AttendanceSession createOrGetAttendanceSession(Teacher teacher, Long offeringId, LocalDate classDate) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        return attendanceSessionRepository.findBySubjectOfferingIdAndClassDate(offeringId, classDate)
                .orElseGet(() -> attendanceSessionRepository.save(AttendanceSession.builder()
                        .subjectOffering(offering)
                        .classDate(classDate)
                        .createdBy(teacher)
                        .locked(false)
                        .createdAt(Instant.now())
                        .build()));
    }

    @Transactional
    public void markAttendance(Teacher teacher, Long offeringId, LocalDate classDate, List<AttendanceMarkInput> marks) {
        AttendanceSession session = createOrGetAttendanceSession(teacher, offeringId, classDate);
        if (session.isLocked()) {
            throw new IllegalStateException("Attendance session is locked");
        }

        for (AttendanceMarkInput mark : marks) {
            Student student = studentRepository.findById(mark.studentId())
                    .orElseThrow(() -> new IllegalArgumentException("Student not found: " + mark.studentId()));
            boolean inRoster = registrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), offeringId)
                    .filter(r -> r.getStatus() == Registration.RegistrationStatus.CONFIRMED
                            || r.getStatus() == Registration.RegistrationStatus.SUBMITTED)
                    .isPresent();
            if (!inRoster) {
                continue;
            }

            Attendance attendance = attendanceRepository.findByStudentIdAndSubjectOfferingId(student.getId(), offeringId).stream()
                    .filter(a -> classDate.equals(a.getDate()))
                    .findFirst()
                    .orElseGet(() -> Attendance.builder()
                            .student(student)
                            .subjectOffering(session.getSubjectOffering())
                            .session(session)
                            .date(classDate)
                            .build());
            attendance.setStatus(mark.status());
            attendance.setReason(mark.reason());
            attendance.setSession(session);
            attendanceRepository.save(attendance);

            notificationService.notifyStudent(
                    student.getEmail(),
                    Notification.NotificationType.ATTENDANCE,
                    "Attendance updated",
                    "Attendance for " + classDate + " in " + session.getSubjectOffering().getSubject().getCode() + " is " + mark.status(),
                    "/portal/class-attendance"
            );
        }
        auditService.logStudentAction(null, "ATTENDANCE_MARKED", "AttendanceSession", session.getId(),
                "offeringId=" + offeringId + ", classDate=" + classDate + ", records=" + marks.size());
    }

    @Transactional
    public AssessmentComponent createComponent(Teacher teacher, Long offeringId, String name, AssessmentComponent.ComponentType type, double weightPercent) {
        if (teacher.getRole() == Teacher.TeacherRole.TA) {
            throw new IllegalStateException("TA cannot create assessment components");
        }
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        AssessmentComponent component = AssessmentComponent.builder()
                .subjectOffering(offering)
                .name(name)
                .type(type)
                .weightPercent(weightPercent)
                .status(AssessmentComponent.ComponentStatus.DRAFT)
                .published(false)
                .locked(false)
                .createdAt(Instant.now())
                .build();
        AssessmentComponent saved = assessmentComponentRepository.save(component);
        auditService.logStudentAction(null, "ASSESSMENT_COMPONENT_CREATED", "AssessmentComponent", saved.getId(), "offeringId=" + offeringId);
        return saved;
    }

    @Transactional
    public Grade saveGrade(
            Teacher teacher,
            Long offeringId,
            Long studentId,
            Long componentId,
            double gradeValue,
            double maxGradeValue,
            String comment) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        AssessmentComponent component = assessmentComponentRepository.findById(componentId)
                .orElseThrow(() -> new IllegalArgumentException("Component not found"));
        if (component.isLocked()) {
            throw new IllegalStateException("Assessment component is locked");
        }
        if (!component.getSubjectOffering().getId().equals(offering.getId())) {
            throw new IllegalArgumentException("Component does not belong to section");
        }

        Grade grade = gradeRepository.findBySubjectOfferingId(offeringId).stream()
                .filter(g -> g.getStudent().getId().equals(studentId))
                .filter(g -> g.getComponent() != null && g.getComponent().getId().equals(componentId))
                .findFirst()
                .orElseGet(() -> Grade.builder()
                        .student(student)
                        .subjectOffering(offering)
                        .component(component)
                        .type(mapType(component.getType()))
                        .createdAt(Instant.now())
                        .published(component.isPublished())
                        .build());

        grade.setGradeValue(gradeValue);
        grade.setMaxGradeValue(maxGradeValue);
        grade.setComment(comment);
        grade.setPublished(component.isPublished());
        Grade saved = gradeRepository.save(grade);

        if (saved.isPublished()) {
            notificationService.notifyStudent(
                    student.getEmail(),
                    Notification.NotificationType.GRADE,
                    "Grade published",
                    component.getName() + ": " + gradeValue + "/" + maxGradeValue,
                    "/portal/student-journal"
            );
        }
        auditService.logStudentAction(null, "GRADE_SAVED", "Grade", saved.getId(),
                "studentId=" + studentId + ", offeringId=" + offeringId + ", componentId=" + componentId + ", published=" + saved.isPublished());
        return saved;
    }

    @Transactional
    public AssessmentComponent setComponentPublishState(Teacher teacher, Long offeringId, Long componentId, boolean published) {
        if (teacher.getRole() == Teacher.TeacherRole.TA) {
            throw new IllegalStateException("TA cannot publish component grades");
        }
        getTeacherOffering(teacher, offeringId);
        AssessmentComponent component = assessmentComponentRepository.findById(componentId)
                .orElseThrow(() -> new IllegalArgumentException("Component not found"));
        if (!component.getSubjectOffering().getId().equals(offeringId)) {
            throw new IllegalArgumentException("Component does not belong to section");
        }
        if (published && !windowPolicyService.isWindowActive(component.getSubjectOffering().getSemester().getId(), RegistrationWindow.WindowType.GRADE_PUBLISH)) {
            throw new IllegalStateException("Grade publish window is closed");
        }

        component.setPublished(published);
        component.setStatus(published ? AssessmentComponent.ComponentStatus.PUBLISHED : AssessmentComponent.ComponentStatus.DRAFT);
        AssessmentComponent saved = assessmentComponentRepository.save(component);

        List<Grade> grades = gradeRepository.findBySubjectOfferingId(offeringId).stream()
                .filter(g -> g.getComponent() != null && g.getComponent().getId().equals(componentId))
                .toList();
        grades.forEach(g -> g.setPublished(published));
        gradeRepository.saveAll(grades);

        if (published) {
            grades.forEach(g -> notificationService.notifyStudent(
                    g.getStudent().getEmail(),
                    Notification.NotificationType.GRADE,
                    "New grades published",
                    "Component " + component.getName() + " has been published",
                    "/portal/student-journal"
            ));
        }
        auditService.logStudentAction(null, published ? "GRADE_COMPONENT_PUBLISHED" : "GRADE_COMPONENT_HIDDEN",
                "AssessmentComponent", componentId, "offeringId=" + offeringId);
        return saved;
    }

    @Transactional
    public AssessmentComponent lockComponent(Teacher teacher, Long offeringId, Long componentId, boolean lock) {
        if (teacher.getRole() == Teacher.TeacherRole.TA) {
            throw new IllegalStateException("TA cannot lock component");
        }
        getTeacherOffering(teacher, offeringId);
        AssessmentComponent component = assessmentComponentRepository.findById(componentId)
                .orElseThrow(() -> new IllegalArgumentException("Component not found"));
        if (!component.getSubjectOffering().getId().equals(offeringId)) {
            throw new IllegalArgumentException("Component does not belong to section");
        }
        component.setLocked(lock);
        component.setStatus(lock ? AssessmentComponent.ComponentStatus.LOCKED
                : (component.isPublished() ? AssessmentComponent.ComponentStatus.PUBLISHED : AssessmentComponent.ComponentStatus.DRAFT));
        AssessmentComponent saved = assessmentComponentRepository.save(component);
        auditService.logStudentAction(null, lock ? "GRADE_COMPONENT_LOCKED" : "GRADE_COMPONENT_UNLOCKED",
                "AssessmentComponent", componentId, "offeringId=" + offeringId);
        return saved;
    }

    @Transactional
    public FinalGrade upsertFinalGrade(Teacher teacher, Long offeringId, Long studentId, double numericValue, String letterValue, double points) {
        if (teacher.getRole() == Teacher.TeacherRole.TA) {
            throw new IllegalStateException("TA cannot upsert final grades");
        }
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        FinalGrade finalGrade = finalGradeRepository.findByStudentIdAndSubjectOfferingId(studentId, offeringId)
                .orElseGet(() -> FinalGrade.builder()
                        .student(student)
                        .subjectOffering(offering)
                        .createdAt(Instant.now())
                        .status(FinalGrade.FinalGradeStatus.CALCULATED)
                        .published(false)
                        .build());

        finalGrade.setNumericValue(numericValue);
        finalGrade.setLetterValue(letterValue);
        finalGrade.setPoints(points);
        if (finalGrade.getStatus() == null || finalGrade.getStatus() == FinalGrade.FinalGradeStatus.PUBLISHED) {
            finalGrade.setStatus(FinalGrade.FinalGradeStatus.CALCULATED);
        }
        finalGrade.setUpdatedAt(Instant.now());
        FinalGrade saved = finalGradeRepository.save(finalGrade);
        auditService.logStudentAction(null, "FINAL_GRADE_UPSERT", "FinalGrade", saved.getId(),
                "studentId=" + studentId + ", offeringId=" + offeringId);
        return saved;
    }

    @Transactional
    public FinalGrade publishFinalGrade(Teacher teacher, Long offeringId, Long studentId) {
        if (teacher.getRole() == Teacher.TeacherRole.TA) {
            throw new IllegalStateException("TA cannot publish final grades");
        }
        getTeacherOffering(teacher, offeringId);
        FinalGrade finalGrade = finalGradeRepository.findByStudentIdAndSubjectOfferingId(studentId, offeringId)
                .orElseThrow(() -> new IllegalArgumentException("Final grade not found"));
        if (!windowPolicyService.isWindowActive(finalGrade.getSubjectOffering().getSemester().getId(), RegistrationWindow.WindowType.GRADE_PUBLISH)) {
            throw new IllegalStateException("Grade publish window is closed");
        }
        finalGrade.setPublished(true);
        finalGrade.setStatus(FinalGrade.FinalGradeStatus.PUBLISHED);
        finalGrade.setPublishedAt(Instant.now());
        finalGrade.setUpdatedAt(Instant.now());
        FinalGrade saved = finalGradeRepository.save(finalGrade);
        notificationService.notifyStudent(
                saved.getStudent().getEmail(),
                Notification.NotificationType.FINAL_GRADE,
                "Final grade published",
                "Final grade for " + saved.getSubjectOffering().getSubject().getCode() + " has been published",
                "/portal/assessment-results"
        );
        auditService.logStudentAction(null, "FINAL_GRADE_PUBLISHED", "FinalGrade", saved.getId(),
                "studentId=" + studentId + ", offeringId=" + offeringId);
        return saved;
    }

    public List<AssessmentComponent> componentsForOffering(Teacher teacher, Long offeringId) {
        getTeacherOffering(teacher, offeringId);
        return assessmentComponentRepository.findBySubjectOfferingIdOrderByCreatedAtAsc(offeringId);
    }

    @Transactional
    public TeacherStudentNote upsertStudentNote(
            Teacher teacher,
            Long offeringId,
            Long studentId,
            String note,
            TeacherStudentNote.RiskFlag riskFlag) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        boolean inRoster = registrationRepository.findByStudentIdAndSubjectOfferingId(studentId, offeringId)
                .filter(r -> r.getStatus() == Registration.RegistrationStatus.CONFIRMED
                        || r.getStatus() == Registration.RegistrationStatus.SUBMITTED)
                .isPresent();
        if (!inRoster) {
            throw new IllegalArgumentException("Student is not in this section");
        }

        TeacherStudentNote existing = teacherStudentNoteRepository
                .findByTeacherIdAndStudentIdAndSubjectOfferingId(teacher.getId(), studentId, offeringId)
                .orElseGet(() -> TeacherStudentNote.builder()
                        .teacher(teacher)
                        .student(student)
                        .subjectOffering(offering)
                        .createdAt(Instant.now())
                        .build());
        existing.setNote(note);
        existing.setRiskFlag(riskFlag);
        existing.setUpdatedAt(Instant.now());
        TeacherStudentNote saved = teacherStudentNoteRepository.save(existing);
        auditService.logStudentAction(null, "TEACHER_STUDENT_NOTE_UPSERT", "TeacherStudentNote", saved.getId(),
                "teacherId=" + teacher.getId() + ", studentId=" + studentId + ", offeringId=" + offeringId + ", risk=" + riskFlag);
        return saved;
    }

    public List<TeacherStudentNote> notesForSection(Teacher teacher, Long offeringId) {
        getTeacherOffering(teacher, offeringId);
        return teacherStudentNoteRepository.findByTeacherIdAndSubjectOfferingIdOrderByCreatedAtDesc(teacher.getId(), offeringId);
    }

    @Transactional
    public FileAsset uploadStudentFile(
            Teacher teacher,
            Long offeringId,
            Long studentId,
            String originalName,
            String storagePath,
            String contentType,
            long sizeBytes) {
        SubjectOffering offering = getTeacherOffering(teacher, offeringId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        boolean inRoster = registrationRepository.findByStudentIdAndSubjectOfferingId(studentId, offeringId)
                .filter(r -> r.getStatus() == Registration.RegistrationStatus.CONFIRMED
                        || r.getStatus() == Registration.RegistrationStatus.SUBMITTED)
                .isPresent();
        if (!inRoster) {
            throw new IllegalArgumentException("Student is not enrolled in this section");
        }

        User uploadedBy = userRepository.findByEmail(teacher.getEmail()).orElse(null);
        FileAsset fileAsset = FileAsset.builder()
                .originalName(originalName)
                .storagePath(storagePath)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .category(FileAsset.FileCategory.STUDENT_FILE)
                .linkedEntityType("Student")
                .linkedEntityId(student.getId())
                .ownerStudent(student)
                .uploadedBy(uploadedBy)
                .uploadedAt(Instant.now())
                .build();
        FileAsset saved = fileAssetRepository.save(fileAsset);

        notificationService.notifyStudent(
                student.getEmail(),
                Notification.NotificationType.SYSTEM,
                "New file added by teacher",
                "A new file was added to your Student Files for " + offering.getSubject().getCode(),
                "/portal/student-files"
        );
        auditService.logUserAction(
                uploadedBy,
                "TEACHER_STUDENT_FILE_UPLOADED",
                "FileAsset",
                saved.getId(),
                "offeringId=" + offeringId + ", studentId=" + studentId + ", path=" + storagePath
        );
        return saved;
    }

    private SubjectOffering getTeacherOffering(Teacher teacher, Long offeringId) {
        SubjectOffering offering = subjectOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (offering.getTeacher() == null || !offering.getTeacher().getId().equals(teacher.getId())) {
            throw new IllegalArgumentException("Section is not assigned to current teacher");
        }
        return offering;
    }

    private Grade.GradeType mapType(AssessmentComponent.ComponentType type) {
        return switch (type) {
            case QUIZ -> Grade.GradeType.QUIZ;
            case MIDTERM -> Grade.GradeType.MIDTERM;
            case FINAL -> Grade.GradeType.FINAL;
            case LAB -> Grade.GradeType.LAB;
            case PROJECT, OTHER -> Grade.GradeType.LAB;
        };
    }

    public record AttendanceMarkInput(Long studentId, Attendance.AttendanceStatus status, String reason) {
    }
}
