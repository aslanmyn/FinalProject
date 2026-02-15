package ru.kors.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;
import ru.kors.finalproject.service.FinancialService;
import ru.kors.finalproject.service.NotificationService;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final MeetingTimeRepository meetingTimeRepository;
    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final RegistrationRepository registrationRepository;
    private final AddDropPeriodRepository addDropPeriodRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final NewsRepository newsRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final RequestMessageRepository requestMessageRepository;
    private final SurveyRepository surveyRepository;
    private final GradeRepository gradeRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final CourseAnnouncementRepository courseAnnouncementRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final TeacherStudentNoteRepository teacherStudentNoteRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
    private final CourseMaterialRepository courseMaterialRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final FinancialService financialService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        LocalDate today = LocalDate.now();

        User admin = User.builder()
                .email("admin@kbtu.kz")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .role(User.UserRole.ADMIN)
                .adminPermissions(java.util.EnumSet.allOf(User.AdminPermission.class))
                .enabled(true)
                .build();
        User profUser = User.builder()
                .email("z.professor@kbtu.kz")
                .password(passwordEncoder.encode("prof123"))
                .fullName("Dr. Z. Professor")
                .role(User.UserRole.PROFESSOR)
                .enabled(true)
                .build();
        User taUser = User.builder()
                .email("t.assistant@kbtu.kz")
                .password(passwordEncoder.encode("ta12345"))
                .fullName("T. Assistant")
                .role(User.UserRole.PROFESSOR)
                .enabled(true)
                .build();
        User studentUser = User.builder()
                .email("a_mustafayev@kbtu.kz")
                .password(passwordEncoder.encode("student123"))
                .fullName("Aidar Mustafayev")
                .role(User.UserRole.STUDENT)
                .enabled(true)
                .build();
        userRepository.saveAll(List.of(admin, profUser, taUser, studentUser));

        Faculty fit = facultyRepository.save(Faculty.builder().name("Faculty of Information Technology").build());
        Faculty sbm = facultyRepository.save(Faculty.builder().name("School of Business and Management").build());

        Semester currentTerm = semesterRepository.save(Semester.builder()
                .name("Spring " + today.getYear())
                .startDate(LocalDate.of(today.getYear(), 1, 15))
                .endDate(LocalDate.of(today.getYear(), 5, 30))
                .current(true)
                .build());

        Program cs = programRepository.save(Program.builder()
                .name("Computer Science")
                .creditLimit(21)
                .faculty(fit)
                .build());
        programRepository.save(Program.builder()
                .name("Software Engineering")
                .creditLimit(21)
                .faculty(fit)
                .build());
        programRepository.save(Program.builder()
                .name("Management")
                .creditLimit(18)
                .faculty(sbm)
                .build());

        Teacher teacher = teacherRepository.save(Teacher.builder()
                .email("z.professor@kbtu.kz")
                .name("Dr. Z. Professor")
                .department("Department of Computer Science")
                .positionTitle("Associate Professor")
                .photoUrl("/images/professor-z.png")
                .publicEmail("z.professor@kbtu.kz")
                .officeRoom("A-413")
                .bio("Teaches core CS courses and supervises capstone projects.")
                .officeHours("Tue 14:00-16:00 (offline), Thu 10:00-11:00 (online)")
                .role(Teacher.TeacherRole.TEACHER)
                .faculty(fit)
                .build());
        teacherRepository.save(Teacher.builder()
                .email("t.assistant@kbtu.kz")
                .name("T. Assistant")
                .department("Department of Computer Science")
                .positionTitle("Teaching Assistant")
                .publicEmail("t.assistant@kbtu.kz")
                .officeRoom("B-112")
                .bio("Supports lab sessions and attendance tracking.")
                .officeHours("Mon 17:00-18:00")
                .role(Teacher.TeacherRole.TA)
                .faculty(fit)
                .build());

        Student student = studentRepository.save(Student.builder()
                .email("a_mustafayev@kbtu.kz")
                .name("Aidar Mustafayev")
                .course(3)
                .groupName("CS-301")
                .status(Student.StudentStatus.ACTIVE)
                .program(cs)
                .faculty(fit)
                .currentSemester(currentTerm)
                .creditsEarned(90)
                .address("Almaty, Kazakhstan")
                .phone("+7 777 123 4567")
                .emergencyContact("Parent: +7 777 987 6543")
                .build());

        Subject math = subjectRepository.save(Subject.builder().code("MATH101").name("Calculus I").credits(4).program(cs).build());
        Subject prog = subjectRepository.save(Subject.builder().code("CS201").name("Programming").credits(4).program(cs).build());
        Subject algo = subjectRepository.save(Subject.builder().code("CS301").name("Algorithms").credits(4).program(cs).build());
        subjectPrerequisiteRepository.save(SubjectPrerequisite.builder().subject(algo).prerequisite(prog).build());

        SubjectOffering mathOff = subjectOfferingRepository.save(SubjectOffering.builder()
                .subject(math)
                .semester(currentTerm)
                .teacher(teacher)
                .capacity(50)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .room("A101")
                .lessonType(SubjectOffering.LessonType.LECTURE)
                .build());
        SubjectOffering progOff = subjectOfferingRepository.save(SubjectOffering.builder()
                .subject(prog)
                .semester(currentTerm)
                .teacher(teacher)
                .capacity(30)
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .startTime(LocalTime.of(11, 0))
                .endTime(LocalTime.of(12, 30))
                .room("B205")
                .lessonType(SubjectOffering.LessonType.PRACTICE)
                .build());
        SubjectOffering algoOff = subjectOfferingRepository.save(SubjectOffering.builder()
                .subject(algo)
                .semester(currentTerm)
                .teacher(teacher)
                .capacity(25)
                .dayOfWeek(DayOfWeek.TUESDAY)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
                .room("B210")
                .lessonType(SubjectOffering.LessonType.LECTURE)
                .build());

        meetingTimeRepository.saveAll(List.of(
                MeetingTime.builder().subjectOffering(mathOff).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30)).room("A101").lessonType(SubjectOffering.LessonType.LECTURE).build(),
                MeetingTime.builder().subjectOffering(progOff).dayOfWeek(DayOfWeek.WEDNESDAY).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(12, 30)).room("B205").lessonType(SubjectOffering.LessonType.PRACTICE).build(),
                MeetingTime.builder().subjectOffering(algoOff).dayOfWeek(DayOfWeek.TUESDAY).startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 30)).room("B210").lessonType(SubjectOffering.LessonType.LECTURE).build()
        ));

        addDropPeriodRepository.save(AddDropPeriod.builder()
                .semester(currentTerm)
                .addStart(today.minusDays(14))
                .addEnd(today.plusDays(14))
                .dropEnd(today.plusDays(14))
                .build());

        registrationWindowRepository.saveAll(List.of(
                RegistrationWindow.builder()
                        .semester(currentTerm)
                        .type(RegistrationWindow.WindowType.REGISTRATION)
                        .startDate(today.minusDays(20))
                        .endDate(today.plusDays(20))
                        .active(true)
                        .build(),
                RegistrationWindow.builder()
                        .semester(currentTerm)
                        .type(RegistrationWindow.WindowType.ADD_DROP)
                        .startDate(today.minusDays(10))
                        .endDate(today.plusDays(10))
                        .active(true)
                        .build(),
                RegistrationWindow.builder()
                        .semester(currentTerm)
                        .type(RegistrationWindow.WindowType.FX)
                        .startDate(today.minusDays(5))
                        .endDate(today.plusDays(25))
                        .active(true)
                        .build(),
                RegistrationWindow.builder()
                        .semester(currentTerm)
                        .type(RegistrationWindow.WindowType.GRADE_PUBLISH)
                        .startDate(today.minusDays(20))
                        .endDate(today.plusDays(20))
                        .active(true)
                        .build()
        ));

        registrationRepository.saveAll(List.of(
                Registration.builder()
                        .student(student)
                        .subjectOffering(mathOff)
                        .status(Registration.RegistrationStatus.CONFIRMED)
                        .createdAt(Instant.now())
                        .build(),
                Registration.builder()
                        .student(student)
                        .subjectOffering(progOff)
                        .status(Registration.RegistrationStatus.CONFIRMED)
                        .createdAt(Instant.now())
                        .build()
        ));

        AssessmentComponent quizComponent = assessmentComponentRepository.save(AssessmentComponent.builder()
                .subjectOffering(mathOff)
                .name("Quiz 1")
                .type(AssessmentComponent.ComponentType.QUIZ)
                .weightPercent(20)
                .status(AssessmentComponent.ComponentStatus.PUBLISHED)
                .published(true)
                .locked(false)
                .createdAt(Instant.now())
                .build());
        AssessmentComponent midtermComponent = assessmentComponentRepository.save(AssessmentComponent.builder()
                .subjectOffering(mathOff)
                .name("Midterm")
                .type(AssessmentComponent.ComponentType.MIDTERM)
                .weightPercent(30)
                .status(AssessmentComponent.ComponentStatus.PUBLISHED)
                .published(true)
                .locked(false)
                .createdAt(Instant.now())
                .build());

        gradeRepository.saveAll(List.of(
                Grade.builder()
                        .student(student)
                        .subjectOffering(mathOff)
                        .component(quizComponent)
                        .type(Grade.GradeType.QUIZ)
                        .gradeValue(8)
                        .maxGradeValue(10)
                        .published(true)
                        .createdAt(Instant.now())
                        .build(),
                Grade.builder()
                        .student(student)
                        .subjectOffering(mathOff)
                        .component(midtermComponent)
                        .type(Grade.GradeType.MIDTERM)
                        .gradeValue(74)
                        .maxGradeValue(100)
                        .published(true)
                        .createdAt(Instant.now())
                        .build()
        ));
        Grade gradeForChange = gradeRepository.save(Grade.builder()
                .student(student)
                .subjectOffering(progOff)
                .type(Grade.GradeType.MIDTERM)
                .gradeValue(61)
                .maxGradeValue(100)
                .published(true)
                .createdAt(Instant.now())
                .build());

        finalGradeRepository.save(FinalGrade.builder()
                .student(student)
                .subjectOffering(mathOff)
                .numericValue(82)
                .letterValue("B")
                .points(3.0)
                .status(FinalGrade.FinalGradeStatus.PUBLISHED)
                .published(true)
                .publishedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        courseAnnouncementRepository.saveAll(List.of(
                CourseAnnouncement.builder()
                        .teacher(teacher)
                        .subjectOffering(mathOff)
                        .title("Lecture moved to A-305")
                        .content("Next lecture is moved due to maintenance in the regular room.")
                        .publicVisible(true)
                        .published(true)
                        .pinned(true)
                        .publishedAt(Instant.now().minusSeconds(6_000))
                        .createdAt(Instant.now().minusSeconds(6_000))
                        .updatedAt(Instant.now().minusSeconds(6_000))
                        .build(),
                CourseAnnouncement.builder()
                        .teacher(teacher)
                        .subjectOffering(progOff)
                        .title("Lab 2 deadline reminder")
                        .content("Please submit Lab 2 by Sunday 23:59.")
                        .publicVisible(false)
                        .published(true)
                        .pinned(false)
                        .publishedAt(Instant.now().minusSeconds(4_000))
                        .createdAt(Instant.now().minusSeconds(4_000))
                        .updatedAt(Instant.now().minusSeconds(4_000))
                        .build()
        ));

        teacherStudentNoteRepository.save(TeacherStudentNote.builder()
                .teacher(teacher)
                .student(student)
                .subjectOffering(mathOff)
                .note("Low attendance trend noticed in week 4. Student invited to office hours.")
                .riskFlag(TeacherStudentNote.RiskFlag.LOW_ATTENDANCE)
                .createdAt(Instant.now().minusSeconds(2_500))
                .updatedAt(Instant.now().minusSeconds(2_500))
                .build());

        gradeChangeRequestRepository.save(GradeChangeRequest.builder()
                .teacher(teacher)
                .student(student)
                .subjectOffering(progOff)
                .grade(gradeForChange)
                .oldValue(61.0)
                .newValue(68.0)
                .reason("Manual re-check after appeal.")
                .status(GradeChangeRequest.RequestStatus.SUBMITTED)
                .createdAt(Instant.now().minusSeconds(1_800))
                .build());

        AttendanceSession attendanceSession = attendanceSessionRepository.save(AttendanceSession.builder()
                .subjectOffering(mathOff)
                .classDate(today.minusDays(1))
                .createdBy(teacher)
                .locked(false)
                .createdAt(Instant.now())
                .build());

        attendanceRepository.save(Attendance.builder()
                .student(student)
                .subjectOffering(mathOff)
                .session(attendanceSession)
                .date(today.minusDays(1))
                .status(Attendance.AttendanceStatus.PRESENT)
                .build());

        Charge tuitionCharge = chargeRepository.save(Charge.builder()
                .student(student)
                .amount(new BigDecimal("150000"))
                .description("Tuition " + currentTerm.getName())
                .dueDate(today.minusDays(20))
                .status(Charge.ChargeStatus.PENDING)
                .build());
        paymentRepository.save(Payment.builder()
                .student(student)
                .amount(new BigDecimal("75000"))
                .date(today.minusDays(10))
                .charge(tuitionCharge)
                .build());

        financialService.refreshFinancialHold(student);

        StudentRequest request = studentRequestRepository.save(StudentRequest.builder()
                .student(student)
                .category("Reference")
                .description("Need enrollment certificate with stamp")
                .status(StudentRequest.RequestStatus.IN_REVIEW)
                .assignedTo(admin)
                .createdAt(Instant.now().minusSeconds(2_400))
                .updatedAt(Instant.now().minusSeconds(900))
                .build());

        requestMessageRepository.saveAll(List.of(
                RequestMessage.builder()
                        .request(request)
                        .sender(studentUser)
                        .message("Please prepare it by Friday if possible.")
                        .createdAt(Instant.now().minusSeconds(2_100))
                        .build(),
                RequestMessage.builder()
                        .request(request)
                        .sender(admin)
                        .message("Received. We are processing your request.")
                        .createdAt(Instant.now().minusSeconds(1_800))
                        .build()
        ));

        fileAssetRepository.save(FileAsset.builder()
                .originalName("student_id_scan.pdf")
                .storagePath("/files/student_id_scan.pdf")
                .contentType("application/pdf")
                .sizeBytes(120_000)
                .category(FileAsset.FileCategory.REQUEST_ATTACHMENT)
                .linkedEntityType("StudentRequest")
                .linkedEntityId(request.getId())
                .ownerStudent(student)
                .uploadedBy(studentUser)
                .uploadedAt(Instant.now().minusSeconds(2_000))
                .build());

        surveyRepository.save(Survey.builder()
                .title("Course Quality Survey")
                .startDate(today.minusDays(1))
                .endDate(today.plusDays(14))
                .anonymous(true)
                .semester(currentTerm)
                .build());

        newsRepository.saveAll(List.of(
                News.builder()
                        .title("Registration and Add/Drop Windows Are Open")
                        .content("Registration and add/drop are active. Check your financial hold status before submitting.")
                        .category("Academic")
                        .createdAt(Instant.now())
                        .build(),
                News.builder()
                        .title("Teacher Gradebook Update")
                        .content("Grade publication workflow with component lock is now enabled in the portal.")
                        .category("System")
                        .createdAt(Instant.now())
                        .build()
        ));

        checklistItemRepository.saveAll(List.of(
                ChecklistItem.builder()
                        .student(student)
                        .title("Submit registration draft")
                        .deadline(today.plusDays(3))
                        .completed(false)
                        .linkToSection("/portal/course-registration")
                        .build(),
                ChecklistItem.builder()
                        .student(student)
                        .title("Complete active survey")
                        .deadline(today.plusDays(7))
                        .completed(false)
                        .linkToSection("/portal/surveys")
                        .build()
        ));

        courseMaterialRepository.saveAll(List.of(
                CourseMaterial.builder()
                        .subjectOffering(mathOff)
                        .uploadedBy(teacher)
                        .title("Lecture 1 Slides")
                        .description("Introduction to Calculus - limits and derivatives")
                        .originalFileName("calculus_lecture1.pdf")
                        .storagePath("/materials/calculus_lecture1.pdf")
                        .contentType("application/pdf")
                        .sizeBytes(2_500_000)
                        .visibility(CourseMaterial.MaterialVisibility.ENROLLED_ONLY)
                        .published(true)
                        .createdAt(Instant.now().minusSeconds(10_000))
                        .updatedAt(Instant.now().minusSeconds(10_000))
                        .build(),
                CourseMaterial.builder()
                        .subjectOffering(progOff)
                        .uploadedBy(teacher)
                        .title("Lab 2 Starter Code")
                        .description("Starter code for the linked list lab assignment")
                        .originalFileName("lab2_starter.zip")
                        .storagePath("/materials/lab2_starter.zip")
                        .contentType("application/zip")
                        .sizeBytes(45_000)
                        .visibility(CourseMaterial.MaterialVisibility.ENROLLED_ONLY)
                        .published(true)
                        .createdAt(Instant.now().minusSeconds(5_000))
                        .updatedAt(Instant.now().minusSeconds(5_000))
                        .build()
        ));

        checklistTemplateRepository.saveAll(List.of(
                ChecklistTemplate.builder()
                        .title("Complete course registration")
                        .linkToSection("/portal/course-registration")
                        .triggerEvent(ChecklistTemplate.TriggerEvent.ENROLLMENT)
                        .offsetDays(7)
                        .active(true)
                        .build(),
                ChecklistTemplate.builder()
                        .title("Review academic schedule")
                        .linkToSection("/portal/student-schedule")
                        .triggerEvent(ChecklistTemplate.TriggerEvent.SEMESTER_START)
                        .offsetDays(3)
                        .active(true)
                        .build(),
                ChecklistTemplate.builder()
                        .title("Complete course evaluation surveys")
                        .linkToSection("/portal/surveys")
                        .triggerEvent(ChecklistTemplate.TriggerEvent.SEMESTER_END)
                        .offsetDays(-7)
                        .active(true)
                        .build()
        ));

        examScheduleRepository.saveAll(List.of(
                ExamSchedule.builder()
                        .subjectOffering(mathOff)
                        .examDate(currentTerm.getEndDate().minusDays(10))
                        .examTime(LocalTime.of(10, 0))
                        .room("Exam Hall A")
                        .format("Written")
                        .build(),
                ExamSchedule.builder()
                        .subjectOffering(progOff)
                        .examDate(currentTerm.getEndDate().minusDays(7))
                        .examTime(LocalTime.of(14, 0))
                        .room("Lab B-201")
                        .format("Practical + Written")
                        .build()
        ));

        notificationService.notifyStudent(
                student.getEmail(),
                Notification.NotificationType.SYSTEM,
                "Portal initialized",
                "Demo data loaded. You can test student, teacher, and admin workflows now.",
                "/news"
        );
    }
}
