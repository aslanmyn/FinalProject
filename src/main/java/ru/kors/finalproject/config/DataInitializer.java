package ru.kors.finalproject.config;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.AddDropPeriod;
import ru.kors.finalproject.entity.AssessmentComponent;
import ru.kors.finalproject.entity.Attendance;
import ru.kors.finalproject.entity.AttendanceSession;
import ru.kors.finalproject.entity.Charge;
import ru.kors.finalproject.entity.ChecklistItem;
import ru.kors.finalproject.entity.ChecklistTemplate;
import ru.kors.finalproject.entity.CourseAnnouncement;
import ru.kors.finalproject.entity.ExamSchedule;
import ru.kors.finalproject.entity.Faculty;
import ru.kors.finalproject.entity.FinalGrade;
import ru.kors.finalproject.entity.Grade;
import ru.kors.finalproject.entity.GradeChangeRequest;
import ru.kors.finalproject.entity.MeetingTime;
import ru.kors.finalproject.entity.News;
import ru.kors.finalproject.entity.Notification;
import ru.kors.finalproject.entity.Payment;
import ru.kors.finalproject.entity.Program;
import ru.kors.finalproject.entity.ProgramCurriculumItem;
import ru.kors.finalproject.entity.Registration;
import ru.kors.finalproject.entity.RegistrationWindow;
import ru.kors.finalproject.entity.RequestMessage;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.StudentRequest;
import ru.kors.finalproject.entity.Subject;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.entity.SubjectPrerequisite;
import ru.kors.finalproject.entity.Survey;
import ru.kors.finalproject.entity.SurveyQuestion;
import ru.kors.finalproject.entity.SurveyResponse;
import ru.kors.finalproject.entity.Teacher;
import ru.kors.finalproject.entity.TeacherStudentNote;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.AddDropPeriodRepository;
import ru.kors.finalproject.repository.AssessmentComponentRepository;
import ru.kors.finalproject.repository.AttendanceRepository;
import ru.kors.finalproject.repository.AttendanceSessionRepository;
import ru.kors.finalproject.repository.ChargeRepository;
import ru.kors.finalproject.repository.ChecklistItemRepository;
import ru.kors.finalproject.repository.ChecklistTemplateRepository;
import ru.kors.finalproject.repository.CourseAnnouncementRepository;
import ru.kors.finalproject.repository.ExamScheduleRepository;
import ru.kors.finalproject.repository.FacultyRepository;
import ru.kors.finalproject.repository.FinalGradeRepository;
import ru.kors.finalproject.repository.GradeChangeRequestRepository;
import ru.kors.finalproject.repository.GradeRepository;
import ru.kors.finalproject.repository.MeetingTimeRepository;
import ru.kors.finalproject.repository.NewsRepository;
import ru.kors.finalproject.repository.NotificationRepository;
import ru.kors.finalproject.repository.PaymentRepository;
import ru.kors.finalproject.repository.ProgramRepository;
import ru.kors.finalproject.repository.ProgramCurriculumItemRepository;
import ru.kors.finalproject.repository.RegistrationRepository;
import ru.kors.finalproject.repository.RegistrationWindowRepository;
import ru.kors.finalproject.repository.RequestMessageRepository;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.StudentRepository;
import ru.kors.finalproject.repository.StudentRequestRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.repository.SubjectPrerequisiteRepository;
import ru.kors.finalproject.repository.SubjectRepository;
import ru.kors.finalproject.repository.SurveyRepository;
import ru.kors.finalproject.repository.SurveyResponseRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.repository.TeacherStudentNoteRepository;
import ru.kors.finalproject.repository.UserRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final String SEED_CONFIRM_TOKEN = "DEMO_ONLY_RESET";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String PROFESSOR_PASSWORD = "prof123";
    private static final String STUDENT_PASSWORD = "student123";
    private static final int STUDENT_COUNT = 100;
    private static final String LOCAL_CREDENTIALS_FILE = "seed-users.local.txt";

    private static final String SITE = "School of Information Technology and Engineering (SITE)";
    private static final String SEOGI = "School of Energy and Oil and Gas Industry (SEOGI)";
    private static final String ISE = "International School of Economics (ISE)";
    private static final String BUSINESS = "KBTU Business School";
    private static final String KMA = "Kazakhstan Maritime Academy (KMA)";
    private static final String SMC = "School of Mathematics and Cybernetics (SMC)";
    private static final String GEO = "School of Geology (GEO)";
    private static final String MATERIALS = "School of Materials Science and Green Technologies";

    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final FacultyRepository facultyRepository;
    private final ProgramRepository programRepository;
    private final ProgramCurriculumItemRepository programCurriculumItemRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final MeetingTimeRepository meetingTimeRepository;
    private final SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final AddDropPeriodRepository addDropPeriodRepository;
    private final AssessmentComponentRepository assessmentComponentRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final NewsRepository newsRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final RequestMessageRepository requestMessageRepository;
    private final SurveyRepository surveyRepository;
    private final SurveyResponseRepository surveyResponseRepository;
    private final CourseAnnouncementRepository courseAnnouncementRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final TeacherStudentNoteRepository teacherStudentNoteRepository;
    private final ChargeRepository chargeRepository;
    private final PaymentRepository paymentRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final NotificationRepository notificationRepository;
    @Value("${app.seed.confirm-token:}")
    private String seedConfirmToken;

    @Override
    @Transactional
    public void run(String... args) {
        if (!SEED_CONFIRM_TOKEN.equals(seedConfirmToken)) {
            System.err.println("=======================================================================");
            System.err.println("SEED ABORTED: missing confirm token.");
            System.err.println("To run demo seed, set APP_SEED_ENABLED=true and APP_SEED_CONFIRM_TOKEN=" + SEED_CONFIRM_TOKEN);
            System.err.println("=======================================================================");
            return;
        }

        if (isLargeDemoDatasetPresent()) {
            writeCredentialsFile(buildKnownCredentials());
            return;
        }

        // SAFETY GUARD: refuse to wipe a database that has real (non-demo) users
        if (hasExistingNonDemoData()) {
            System.err.println("=======================================================================");
            System.err.println("SEED ABORTED: Database contains existing user data that is NOT demo data.");
            System.err.println("Refusing to TRUNCATE to protect production/staging data.");
            System.err.println("Set APP_SEED_ENABLED=false or clear the database manually.");
            System.err.println("=======================================================================");
            return;
        }

        resetDatabase();

        SeedContext context = new SeedContext();
        context.today = LocalDate.now();
        context.now = Instant.now();
        context.curriculum = buildCurriculum();

        seedFacultiesAndPrograms(context);
        seedSemestersAndWindows(context);
        seedAdmins(context);
        seedTeachers(context);
        seedSubjects(context);
        seedProgramCurriculum(context);
        seedPrerequisites(context);
        seedStudentsAndAcademics(context);
        seedNextSemesterOfferings(context);
        seedNews(context);
        seedRequests(context);
        seedSurvey(context);
        seedChecklists(context);
        seedCurrentCourseArtifacts(context);
        seedFinance(context);
        seedNotifications(context);

        entityManager.flush();
        writeCredentialsFile(context.credentials);
    }

    private boolean isLargeDemoDatasetPresent() {
        return facultyRepository.findByName(SITE).isPresent()
                && teacherRepository.findByEmail(DemoIdentitySupport.teacherEmailFromFullName("Professor Aidos Nurgaliyev")).isPresent()
                && studentRepository.count() >= STUDENT_COUNT
                && programCurriculumItemRepository.count() > 0
                && hasUpdatedSchedulePatterns();
    }

    private boolean isDemoDatasetCompatibleForReset() {
        return facultyRepository.findByName(SITE).isPresent()
                && userRepository.findByEmail("admin@kbtu.kz").isPresent()
                && teacherRepository.findByEmail(DemoIdentitySupport.teacherEmailFromFullName("Professor Aidos Nurgaliyev")).isPresent()
                && semesterRepository.count() >= 8
                && studentRepository.count() >= STUDENT_COUNT / 2;
    }

    private boolean hasUpdatedSchedulePatterns() {
        return subjectOfferingRepository.findBySemesterIdWithDetails(8L).stream()
                .filter(offering -> offering.getSubject() != null)
                .filter(offering -> {
                    String code = offering.getSubject().getCode();
                    return code != null && (code.startsWith("LAN") || code.startsWith("HUM") || code.startsWith("PHE"));
                })
                .anyMatch(offering -> offering.getMeetingTimes() != null
                        && offering.getMeetingTimes().size() >= 3
                        && offering.getMeetingTimes().stream().allMatch(slot ->
                        slot.getLessonType() == SubjectOffering.LessonType.PRACTICE
                                && slot.getStartTime() != null
                                && slot.getEndTime() != null
                                && java.time.Duration.between(slot.getStartTime(), slot.getEndTime()).toHours() == 1));
    }

    /**
     * Returns true if the database has users that are NOT part of the demo dataset.
     * This prevents the TRUNCATE from destroying real production data.
     */
    private boolean hasExistingNonDemoData() {
        long userCount = userRepository.count();
        if (userCount == 0) {
            return false; // empty database - safe to seed
        }
        if (isDemoDatasetCompatibleForReset()) {
            return false;
        }
        return true;
    }

    private void resetDatabase() {
        entityManager.createNativeQuery("""
                TRUNCATE TABLE
                    audit_logs,
                    chat_messages,
                    chat_room_members,
                    chat_rooms,
                    clearance_checkpoints,
                    clearance_sheets,
                    course_announcements,
                    course_materials,
                    exam_schedules,
                    file_assets,
                    final_grades,
                    fx_registrations,
                    grade_change_requests,
                    grades,
                    holds,
                    meeting_times,
                    mobility_applications,
                    notifications,
                    payments,
                    charges,
                    refresh_tokens,
                    program_curriculum_items,
                    planned_registrations,
                    registrations,
                    registration_windows,
                    add_drop_periods,
                    request_messages,
                    social_activities,
                    student_files,
                    student_requests,
                    survey_responses,
                    survey_questions,
                    surveys,
                    assessment_components,
                    attendance_sessions,
                    attendances,
                    checklist_items,
                    checklist_templates,
                    subject_prerequisites,
                    subject_offerings,
                    subjects,
                    students,
                    teachers,
                    programs,
                    faculties,
                    news,
                    semesters,
                    users,
                    user_admin_permissions
                RESTART IDENTITY CASCADE
                """).executeUpdate();
        entityManager.clear();
    }

    private void seedFacultiesAndPrograms(SeedContext context) {
        for (String facultyName : facultyNames()) {
            Faculty faculty = facultyRepository.save(Faculty.builder().name(facultyName).build());
            context.faculties.put(facultyName, faculty);

            Program program = programRepository.save(Program.builder()
                    .name(facultyProgramName(facultyName))
                    .creditLimit(22)
                    .faculty(faculty)
                    .build());
            context.programs.put(facultyName, program);
        }
    }

    private void seedSemestersAndWindows(SeedContext context) {
        List<SemesterSpec> specs = List.of(
                new SemesterSpec(1, "2022-2023 Fall", LocalDate.of(2022, 9, 1), LocalDate.of(2022, 12, 20), false),
                new SemesterSpec(2, "2022-2023 Spring", LocalDate.of(2023, 1, 16), LocalDate.of(2023, 5, 25), false),
                new SemesterSpec(3, "2023-2024 Fall", LocalDate.of(2023, 9, 1), LocalDate.of(2023, 12, 20), false),
                new SemesterSpec(4, "2023-2024 Spring", LocalDate.of(2024, 1, 15), LocalDate.of(2024, 5, 25), false),
                new SemesterSpec(5, "2024-2025 Fall", LocalDate.of(2024, 9, 1), LocalDate.of(2024, 12, 20), false),
                new SemesterSpec(6, "2024-2025 Spring", LocalDate.of(2025, 1, 15), LocalDate.of(2025, 5, 25), false),
                new SemesterSpec(7, "2025-2026 Fall", LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 20), false),
                new SemesterSpec(8, "2025-2026 Spring", LocalDate.of(2026, 1, 19), LocalDate.of(2026, 5, 29), true),
                new SemesterSpec(9, "2026-2027 Fall", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20), false)
        );
        for (SemesterSpec spec : specs) {
            Semester semester = semesterRepository.save(Semester.builder()
                    .name(spec.name())
                    .startDate(spec.start())
                    .endDate(spec.end())
                    .current(spec.current())
                    .build());
            context.semesters.put(spec.index(), semester);
        }

        Semester currentSemester = context.currentSemester();
        registrationWindowRepository.saveAll(List.of(
                RegistrationWindow.builder()
                        .semester(currentSemester)
                        .type(RegistrationWindow.WindowType.REGISTRATION)
                        .startDate(context.today.minusDays(20))
                        .endDate(context.today.plusDays(10))
                        .active(true)
                        .build(),
                RegistrationWindow.builder()
                        .semester(currentSemester)
                        .type(RegistrationWindow.WindowType.ADD_DROP)
                        .startDate(context.today.minusDays(7))
                        .endDate(context.today.plusDays(7))
                        .active(true)
                        .build(),
                RegistrationWindow.builder()
                        .semester(currentSemester)
                        .type(RegistrationWindow.WindowType.FX)
                        .startDate(currentSemester.getEndDate().minusDays(15))
                        .endDate(currentSemester.getEndDate().plusDays(10))
                        .active(false)
                        .build(),
                RegistrationWindow.builder()
                        .semester(currentSemester)
                        .type(RegistrationWindow.WindowType.GRADE_PUBLISH)
                        .startDate(currentSemester.getEndDate().minusDays(7))
                        .endDate(currentSemester.getEndDate().plusDays(7))
                        .active(false)
                        .build()
        ));

        addDropPeriodRepository.save(AddDropPeriod.builder()
                .semester(currentSemester)
                .addStart(context.today.minusDays(7))
                .addEnd(context.today.plusDays(7))
                .dropEnd(context.today.plusDays(7))
                .build());
    }

    private void seedAdmins(SeedContext context) {
        createAdmin(context, "admin@kbtu.kz", "System Administrator", EnumSet.allOf(User.AdminPermission.class));
        createAdmin(context, "registrar@kbtu.kz", "Registrar Office", EnumSet.of(User.AdminPermission.REGISTRAR));
        createAdmin(context, "finance@kbtu.kz", "Finance Office", EnumSet.of(User.AdminPermission.FINANCE));
        createAdmin(context, "support@kbtu.kz", "Student Support Office", EnumSet.of(User.AdminPermission.SUPPORT, User.AdminPermission.CONTENT));
        createAdmin(context, "mobility@kbtu.kz", "Mobility Office", EnumSet.of(User.AdminPermission.MOBILITY));
    }

    private void createAdmin(SeedContext context, String email, String fullName, Set<User.AdminPermission> permissions) {
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .fullName(fullName)
                .role(User.UserRole.ADMIN)
                .adminPermissions(permissions)
                .enabled(true)
                .build());
        context.usersByEmail.put(email, user);
        context.credentials.add(new AccountCredential("ADMIN", email, ADMIN_PASSWORD, fullName, permissions.toString()));
    }
    private void seedTeachers(SeedContext context) {
        int index = 0;
        for (TeacherSeed seed : teacherSeeds()) {
            String email = DemoIdentitySupport.teacherEmailFromFullName(seed.name());
            User user = userRepository.save(User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(PROFESSOR_PASSWORD))
                    .fullName(seed.name())
                    .role(User.UserRole.PROFESSOR)
                    .enabled(true)
                    .build());
            context.usersByEmail.put(email, user);

            Teacher teacher = teacherRepository.save(Teacher.builder()
                    .email(email)
                    .name(seed.name())
                    .department(departmentForFaculty(seed.facultyName()))
                    .positionTitle("Professor")
                    .photoUrl(null)
                    .publicEmail(email)
                    .officeRoom(roomForFaculty(seed.facultyName(), index))
                    .bio("Leads applied and research-oriented courses for " + seed.facultyName() + ".")
                    .officeHours(index % 2 == 0 ? "Tue 10:00-12:00, Thu 14:00-15:00" : "Mon 15:00-17:00, Wed 11:00-12:00")
                    .role(Teacher.TeacherRole.TEACHER)
                    .faculty(context.faculties.get(seed.facultyName()))
                    .build());
            context.teachersByFaculty.computeIfAbsent(seed.facultyName(), key -> new ArrayList<>()).add(teacher);
            context.credentials.add(new AccountCredential("PROFESSOR", email, PROFESSOR_PASSWORD, seed.name(), seed.facultyName()));
            index++;
        }
    }

    private void seedSubjects(SeedContext context) {
        for (SubjectSeed seed : subjectCatalog()) {
            String homeFaculty = inferHomeFaculty(seed.code(), seed.name());
            Subject subject = subjectRepository.save(Subject.builder()
                    .code(seed.code())
                    .name(seed.name())
                    .credits(defaultCredits(seed.code(), seed.name()))
                    .program(context.programs.get(homeFaculty))
                    .build());
            context.subjects.put(seed.code(), subject);
            context.subjectHomeFaculty.put(seed.code(), homeFaculty);
        }
    }

    private void seedProgramCurriculum(SeedContext context) {
        for (Map.Entry<String, List<List<String>>> entry : context.curriculum.entrySet()) {
            Program program = context.programs.get(entry.getKey());
            if (program == null) {
                continue;
            }
            List<List<String>> slots = entry.getValue();
            for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
                int academicYear = (slotIndex / 2) + 1;
                int semesterNumber = (slotIndex % 2) + 1;
                List<String> subjectCodes = slots.get(slotIndex);
                for (int order = 0; order < subjectCodes.size(); order++) {
                    Subject subject = context.subjects.get(subjectCodes.get(order));
                    if (subject == null) {
                        continue;
                    }
                    programCurriculumItemRepository.save(ProgramCurriculumItem.builder()
                            .program(program)
                            .subject(subject)
                            .academicYear(academicYear)
                            .semesterNumber(semesterNumber)
                            .displayOrder(order + 1)
                            .required(true)
                            .build());
                }
            }
        }
    }

    private void seedPrerequisites(SeedContext context) {
        savePrerequisite(context, "MATH1202", "MATH1102");
        savePrerequisite(context, "CSCI1204", "CSCI1101");
        savePrerequisite(context, "CSCI2105", "CSCI1102");
        savePrerequisite(context, "CSCI2104", "CSCI1204");
        savePrerequisite(context, "CSCI2208", "CSCI2106");
        savePrerequisite(context, "CSCI3110", "CSCI2107");
        savePrerequisite(context, "INFT3131", "CSCI2104");
        savePrerequisite(context, "INFT3132", "INFT3131");
        savePrerequisite(context, "INFT3134", "INFT3131");
        savePrerequisite(context, "INFT3135", "CSCI2106");
        savePrerequisite(context, "INFT3139", "INFT2205");
        savePrerequisite(context, "INFT3140", "INFT2205");
        savePrerequisite(context, "INFS3233", "CSCI2107");
        savePrerequisite(context, "INFS4137", "CSCI3110");
        savePrerequisite(context, "INFS4145", "INFS3233");
        savePrerequisite(context, "CSCI3237", "CSCI2105");
        savePrerequisite(context, "CSCI3234", "CSCI3237");
        savePrerequisite(context, "ISE1322", "ISE1321");
        savePrerequisite(context, "CSE1362", "CSE1366");
        savePrerequisite(context, "FIN1319", "FIN1202");
        savePrerequisite(context, "FIN1311", "FIN1202");
        savePrerequisite(context, "PET1405", "PET1312");
    }

    private void savePrerequisite(SeedContext context, String subjectCode, String prerequisiteCode) {
        Subject subject = context.subjects.get(subjectCode);
        Subject prerequisite = context.subjects.get(prerequisiteCode);
        if (subject == null || prerequisite == null) {
            return;
        }
        subjectPrerequisiteRepository.save(SubjectPrerequisite.builder()
                .subject(subject)
                .prerequisite(prerequisite)
                .build());
    }

    private void seedNextSemesterOfferings(SeedContext context) {
        Semester nextSemester = context.semesters.get(9);
        if (nextSemester == null) {
            return;
        }
        // For each faculty, create offerings for the next curriculum slot after each student's current slot.
        // We collect all unique subject codes that any student would take next semester.
        java.util.Set<String> nextSubjectCodes = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, List<List<String>>> entry : context.curriculum.entrySet()) {
            List<List<String>> slots = entry.getValue();
            // Students at course years 1-3 get next-semester offerings.
            // Course 4 students only have diploma/practice defence, no regular schedule.
            for (int course = 1; course <= 3; course++) {
                int nextSlotIndex = course * 2; // 0-based index for the next slot
                if (nextSlotIndex < slots.size()) {
                    nextSubjectCodes.addAll(slots.get(nextSlotIndex));
                }
            }
        }

        // Create offerings with meeting times for each unique subject in the next semester
        for (String subjectCode : nextSubjectCodes) {
            Subject subject = context.subjects.get(subjectCode);
            if (subject == null) {
                continue;
            }
            getOrCreateOffering(context, subject, nextSemester);
        }

        seedAlternativeNextSemesterOfferings(context, nextSemester);
    }

    private void seedAlternativeNextSemesterOfferings(SeedContext context, Semester nextSemester) {
        createOfferingVariant(
                context,
                nextSemester,
                "CSCI2104",
                "csci2104-alt-a",
                "r.serikbayev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(10, 0), "L-427", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.THURSDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), "P-427", SubjectOffering.LessonType.PRACTICE)
                )
        );
        createOfferingVariant(
                context,
                nextSemester,
                "CSCI2104",
                "csci2104-alt-b",
                "a.nurgaliyev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "L-427", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "P-427", SubjectOffering.LessonType.PRACTICE)
                )
        );

        createOfferingVariant(
                context,
                nextSemester,
                "CSCI2105",
                "csci2105-alt-a",
                "a.nurgaliyev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.TUESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "L-428", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.FRIDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "P-428", SubjectOffering.LessonType.PRACTICE)
                )
        );
        createOfferingVariant(
                context,
                nextSemester,
                "CSCI2105",
                "csci2105-alt-b",
                "r.serikbayev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(18, 0), "L-428", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "P-428", SubjectOffering.LessonType.PRACTICE)
                )
        );

        createOfferingVariant(
                context,
                nextSemester,
                "INFT2102",
                "inft2102-alt-a",
                "r.serikbayev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(12, 0), "L-418", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), "P-418", SubjectOffering.LessonType.PRACTICE)
                )
        );
        createOfferingVariant(
                context,
                nextSemester,
                "INFT2102",
                "inft2102-alt-b",
                "a.nurgaliyev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.WEDNESDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), "L-418", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), "P-418", SubjectOffering.LessonType.PRACTICE)
                )
        );

        createOfferingVariant(
                context,
                nextSemester,
                "INFT2205",
                "inft2205-alt-a",
                "a.nurgaliyev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(14, 0), "L-382", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.THURSDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), "P-382", SubjectOffering.LessonType.PRACTICE)
                )
        );
        createOfferingVariant(
                context,
                nextSemester,
                "INFT2205",
                "inft2205-alt-b",
                "r.serikbayev@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.TUESDAY, LocalTime.of(16, 0), LocalTime.of(18, 0), "L-382", SubjectOffering.LessonType.LECTURE),
                        new MeetingSlotSeed(DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "P-382", SubjectOffering.LessonType.PRACTICE)
                )
        );

        createOfferingVariant(
                context,
                nextSemester,
                "HUM1101",
                "hum1101-alt-a",
                "a.beketov@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "P-379", SubjectOffering.LessonType.PRACTICE),
                        new MeetingSlotSeed(DayOfWeek.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(13, 0), "P-379", SubjectOffering.LessonType.PRACTICE),
                        new MeetingSlotSeed(DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), "P-379", SubjectOffering.LessonType.PRACTICE)
                )
        );
        createOfferingVariant(
                context,
                nextSemester,
                "HUM1101",
                "hum1101-alt-b",
                "a.beketov@kbtu.kz",
                List.of(
                        new MeetingSlotSeed(DayOfWeek.TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), "P-379", SubjectOffering.LessonType.PRACTICE),
                        new MeetingSlotSeed(DayOfWeek.THURSDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), "P-379", SubjectOffering.LessonType.PRACTICE),
                        new MeetingSlotSeed(DayOfWeek.FRIDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), "P-379", SubjectOffering.LessonType.PRACTICE)
                )
        );
    }

    private void seedStudentsAndAcademics(SeedContext context) {
        int studentIndex = 1;
        List<String> facultyNames = facultyNames();
        for (int course = 1; course <= 4; course++) {
            for (String facultyName : facultyNames) {
                for (int copy = 0; copy < 3; copy++) {
                    createStudentWithAcademics(context, studentIndex++, facultyName, course);
                }
            }
            createStudentWithAcademics(context, studentIndex++, facultyNames.get(course - 1), course);
        }
    }

    private void createStudentWithAcademics(SeedContext context, int studentIndex, String facultyName, int course) {
        String fullName = DemoIdentitySupport.generateStudentName(studentIndex);
        String email = DemoIdentitySupport.studentEmailFromFullName(fullName);
        User user = userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(STUDENT_PASSWORD))
                .fullName(fullName)
                .role(User.UserRole.STUDENT)
                .enabled(true)
                .build());
        context.usersByEmail.put(email, user);

        Student student = studentRepository.save(Student.builder()
                .email(email)
                .name(fullName)
                .course(course)
                .groupName("")
                .status(Student.StudentStatus.ACTIVE)
                .program(context.programs.get(facultyName))
                .faculty(context.faculties.get(facultyName))
                .currentSemester(context.currentSemester())
                .creditsEarned(0)
                .passportNumber(String.format(Locale.ROOT, "KZ%07d", 5000000 + studentIndex))
                .address("Almaty, Kazakhstan")
                .phone(String.format(Locale.ROOT, "+7 701 %03d %04d", 100 + studentIndex, 2000 + studentIndex))
                .emergencyContact("Family contact")
                .build());
        context.students.add(student);
        context.studentCurrentSlot.put(student.getId(), course * 2);
        context.studentAbility.put(student.getId(), 52.0 + Math.floorMod(Objects.hash(student.getEmail(), facultyName), 41));
        context.studentAttendanceBias.put(student.getId(), 0.64 + (Math.floorMod(Objects.hash(student.getEmail(), "attendance"), 31) / 100.0));
        context.credentials.add(new AccountCredential("STUDENT", email, STUDENT_PASSWORD, fullName, facultyName + ", year " + course));

        int creditsEarned = seedAcademicPathForStudent(context, student, facultyName, course);
        student.setCreditsEarned(creditsEarned);
        studentRepository.save(student);
    }

    private int seedAcademicPathForStudent(SeedContext context, Student student, String facultyName, int course) {
        List<List<String>> curriculum = context.curriculum.get(facultyName);
        int currentSlot = course * 2;
        int earnedCredits = 0;
        for (int slot = 1; slot <= currentSlot; slot++) {
            int semesterIndex = 8 - (currentSlot - slot);
            Semester semester = context.semesters.get(semesterIndex);
            boolean currentSlotFlag = slot == currentSlot;
            for (String subjectCode : curriculum.get(slot - 1)) {
                Subject subject = context.subjects.get(subjectCode);
                if (subject == null) {
                    continue;
                }
                SubjectOffering offering = getOrCreateOffering(context, subject, semester);
                Registration registration = registrationRepository.save(Registration.builder()
                        .student(student)
                        .subjectOffering(offering)
                        .status(Registration.RegistrationStatus.CONFIRMED)
                        .createdAt(startOfDay(semester.getStartDate().plusDays(2)))
                        .build());
                if (currentSlotFlag) {
                    context.currentRegistrationsByOffering.computeIfAbsent(offering.getId(), key -> new ArrayList<>()).add(registration);
                    double currentCoursework = seedCurrentGrades(context, student, offering, semester);
                    double attendanceRatio = seedAttendance(context, student, offering, semester);
                    context.currentCourseworkTotals.put(studentOfferingKey(student.getId(), offering.getId()), currentCoursework);
                    context.attendanceRatios.put(studentOfferingKey(student.getId(), offering.getId()), attendanceRatio);
                } else {
                    seedHistoricalGrades(context, student, offering, semester);
                    earnedCredits += subject.getCredits();
                }
            }
        }
        return earnedCredits;
    }

    private SubjectOffering getOrCreateOffering(SeedContext context, Subject subject, Semester semester) {
        String key = subject.getCode() + "|" + semester.getName();
        return createOffering(
                context,
                key,
                subject,
                semester,
                pickTeacher(context, subject.getCode()),
                20,
                buildMeetingPattern(subject)
        );
    }

    private void createOfferingVariant(
            SeedContext context,
            Semester semester,
            String subjectCode,
            String keySuffix,
            String teacherEmail,
            List<MeetingSlotSeed> slots
    ) {
        Subject subject = context.subjects.get(subjectCode);
        if (subject == null) {
            return;
        }
        Teacher teacher = findTeacherByEmail(context, teacherEmail);
        createOffering(
                context,
                subjectCode + "|" + semester.getName() + "|" + keySuffix,
                subject,
                semester,
                teacher != null ? teacher : pickTeacher(context, subjectCode),
                20,
                slots
        );
    }

    private Teacher findTeacherByEmail(SeedContext context, String email) {
        return allTeachers(context).stream()
                .filter(teacher -> Objects.equals(teacher.getEmail(), email))
                .findFirst()
                .orElse(null);
    }

    private SubjectOffering createOffering(
            SeedContext context,
            String key,
            Subject subject,
            Semester semester,
            Teacher teacher,
            int capacity,
            List<MeetingSlotSeed> slots
    ) {
        SubjectOffering existing = context.offerings.get(key);
        if (existing != null) {
            return existing;
        }

        MeetingSlotSeed primarySlot = slots.get(0);
        SubjectOffering offering = subjectOfferingRepository.save(SubjectOffering.builder()
                .subject(subject)
                .semester(semester)
                .teacher(teacher)
                .capacity(capacity)
                .dayOfWeek(primarySlot.dayOfWeek())
                .startTime(primarySlot.startTime())
                .endTime(primarySlot.endTime())
                .room(primarySlot.room())
                .lessonType(primarySlot.lessonType())
                .build());

        List<MeetingTime> meetingTimes = slots.stream()
                .map(slot -> MeetingTime.builder()
                        .subjectOffering(offering)
                        .dayOfWeek(slot.dayOfWeek())
                        .startTime(slot.startTime())
                        .endTime(slot.endTime())
                        .room(slot.room())
                        .lessonType(slot.lessonType())
                        .build())
                .toList();
        meetingTimeRepository.saveAll(meetingTimes);
        offering.setMeetingTimes(new ArrayList<>(meetingTimes));

        List<AssessmentComponent> components = createComponentsForOffering(offering, semester.isCurrent(), semester.getStartDate());
        context.componentsByOffering.put(offering.getId(), components);

        if (semester.isCurrent()) {
            List<AttendanceSession> sessions = createAttendanceSessions(offering, teacher, semester.getStartDate(), primarySlot.dayOfWeek());
            context.attendanceSessionsByOffering.put(offering.getId(), sessions);
            examScheduleRepository.save(ExamSchedule.builder()
                    .subjectOffering(offering)
                    .examDate(semester.getEndDate().minusDays(12 + Math.floorMod(subject.getCode().hashCode(), 8)))
                    .examTime(LocalTime.of(10 + Math.floorMod(subject.getCode().hashCode(), 4) * 2, 0))
                    .room(primarySlot.room())
                    .format(Math.floorMod(subject.getCode().hashCode(), 3) == 0 ? "Written" : "Computer-based")
                    .build());
        }

        context.offerings.put(key, offering);
        return offering;
    }

    private List<AssessmentComponent> createComponentsForOffering(SubjectOffering offering, boolean currentSemester, LocalDate semesterStart) {
        List<AssessmentComponent> components = new ArrayList<>();
        components.add(assessmentComponentRepository.save(AssessmentComponent.builder()
                .subjectOffering(offering)
                .name("Attestation 1")
                .type(AssessmentComponent.ComponentType.MIDTERM)
                .weightPercent(30)
                .status(currentSemester ? AssessmentComponent.ComponentStatus.PUBLISHED : AssessmentComponent.ComponentStatus.LOCKED)
                .published(true)
                .locked(!currentSemester)
                .createdAt(startOfDay(semesterStart.plusDays(10)))
                .build()));
        components.add(assessmentComponentRepository.save(AssessmentComponent.builder()
                .subjectOffering(offering)
                .name("Attestation 2")
                .type(AssessmentComponent.ComponentType.MIDTERM)
                .weightPercent(30)
                .status(currentSemester ? AssessmentComponent.ComponentStatus.PUBLISHED : AssessmentComponent.ComponentStatus.LOCKED)
                .published(true)
                .locked(!currentSemester)
                .createdAt(startOfDay(semesterStart.plusDays(45)))
                .build()));
        components.add(assessmentComponentRepository.save(AssessmentComponent.builder()
                .subjectOffering(offering)
                .name("Final Exam")
                .type(AssessmentComponent.ComponentType.FINAL)
                .weightPercent(40)
                .status(currentSemester ? AssessmentComponent.ComponentStatus.DRAFT : AssessmentComponent.ComponentStatus.LOCKED)
                .published(!currentSemester)
                .locked(!currentSemester)
                .createdAt(startOfDay(semesterStart.plusDays(80)))
                .build()));
        return components;
    }

    private List<AttendanceSession> createAttendanceSessions(SubjectOffering offering, Teacher teacher, LocalDate semesterStart, DayOfWeek lectureDay) {
        List<AttendanceSession> sessions = new ArrayList<>();
        LocalDate alignedStart = semesterStart.with(TemporalAdjusters.nextOrSame(lectureDay));
        for (int week = 1; week <= 6; week++) {
            AttendanceSession session = attendanceSessionRepository.save(AttendanceSession.builder()
                    .subjectOffering(offering)
                    .classDate(alignedStart.plusWeeks(week))
                    .createdBy(teacher)
                    .locked(false)
                    .createdAt(startOfDay(alignedStart.plusWeeks(week)))
                    .build());
            sessions.add(session);
        }
        return sessions;
    }

    private double seedCurrentGrades(SeedContext context, Student student, SubjectOffering offering, Semester semester) {
        List<AssessmentComponent> components = context.componentsByOffering.get(offering.getId());
        double target = clamp(scoreTarget(context, student, offering.getSubject().getCode()) * 0.62, 24, 59);
        double att1 = clamp(roundOne(target * 0.48 + centeredVariation(student.getEmail(), offering.getSubject().getCode(), 2.2)), 8, 30);
        double att2 = clamp(roundOne(target - att1), 8, 30);

        gradeRepository.save(Grade.builder()
                .student(student)
                .subjectOffering(offering)
                .component(components.get(0))
                .type(Grade.GradeType.MIDTERM)
                .gradeValue(att1)
                .maxGradeValue(30)
                .comment("Published after attestation week 1")
                .published(true)
                .createdAt(startOfDay(semester.getStartDate().plusDays(25)))
                .build());
        gradeRepository.save(Grade.builder()
                .student(student)
                .subjectOffering(offering)
                .component(components.get(1))
                .type(Grade.GradeType.MIDTERM)
                .gradeValue(att2)
                .maxGradeValue(30)
                .comment("Published after attestation week 2")
                .published(true)
                .createdAt(startOfDay(semester.getStartDate().plusDays(58)))
                .build());

        context.notifications.add(Notification.builder()
                .recipientEmail(student.getEmail())
                .title("New grades published")
                .message(offering.getSubject().getCode() + " updated with attestation results.")
                .link("/app/student/journal")
                .type(Notification.NotificationType.GRADE)
                .read(false)
                .createdAt(context.now.minusSeconds(Math.floorMod(Objects.hash(student.getEmail(), offering.getId()), 86400)))
                .build());
        return roundOne(att1 + att2);
    }
    private void seedHistoricalGrades(SeedContext context, Student student, SubjectOffering offering, Semester semester) {
        List<AssessmentComponent> components = context.componentsByOffering.get(offering.getId());
        double total = scoreTarget(context, student, offering.getSubject().getCode());
        double att1 = clamp(roundOne(total * 0.29 + centeredVariation(student.getEmail(), offering.getSubject().getCode() + "A1", 2.0)), 0, 30);
        double att2 = clamp(roundOne(total * 0.31 + centeredVariation(student.getEmail(), offering.getSubject().getCode() + "A2", 2.0)), 0, 30);
        double finalExam = clamp(roundOne(total - att1 - att2), 0, 40);
        double normalized = roundOne(att1 + att2 + finalExam);
        GradeScale scale = gradeScale(normalized);

        gradeRepository.saveAll(List.of(
                Grade.builder()
                        .student(student)
                        .subjectOffering(offering)
                        .component(components.get(0))
                        .type(Grade.GradeType.MIDTERM)
                        .gradeValue(att1)
                        .maxGradeValue(30)
                        .comment("Attestation 1")
                        .published(true)
                        .createdAt(startOfDay(semester.getStartDate().plusDays(25)))
                        .build(),
                Grade.builder()
                        .student(student)
                        .subjectOffering(offering)
                        .component(components.get(1))
                        .type(Grade.GradeType.MIDTERM)
                        .gradeValue(att2)
                        .maxGradeValue(30)
                        .comment("Attestation 2")
                        .published(true)
                        .createdAt(startOfDay(semester.getStartDate().plusDays(52)))
                        .build(),
                Grade.builder()
                        .student(student)
                        .subjectOffering(offering)
                        .component(components.get(2))
                        .type(Grade.GradeType.FINAL)
                        .gradeValue(finalExam)
                        .maxGradeValue(40)
                        .comment("Final exam")
                        .published(true)
                        .createdAt(startOfDay(semester.getEndDate().minusDays(7)))
                        .build()
        ));

        finalGradeRepository.save(FinalGrade.builder()
                .student(student)
                .subjectOffering(offering)
                .numericValue(normalized)
                .letterValue(scale.letter())
                .points(scale.points())
                .status(FinalGrade.FinalGradeStatus.PUBLISHED)
                .published(true)
                .publishedAt(startOfDay(semester.getEndDate().minusDays(2)))
                .createdAt(startOfDay(semester.getEndDate().minusDays(7)))
                .updatedAt(startOfDay(semester.getEndDate().minusDays(2)))
                .build());
    }

    private double seedAttendance(SeedContext context, Student student, SubjectOffering offering, Semester semester) {
        List<AttendanceSession> sessions = context.attendanceSessionsByOffering.get(offering.getId());
        if (sessions == null || sessions.isEmpty()) {
            return 1.0;
        }
        int presentCount = 0;
        double attendanceBias = context.studentAttendanceBias.get(student.getId());
        for (AttendanceSession session : sessions) {
            double roll = Math.floorMod(Objects.hash(student.getEmail(), offering.getSubject().getCode(), session.getClassDate()), 100) / 100.0;
            Attendance.AttendanceStatus status;
            if (roll < attendanceBias - 0.08) {
                status = Attendance.AttendanceStatus.PRESENT;
                presentCount++;
            } else if (roll < attendanceBias + 0.06) {
                status = Attendance.AttendanceStatus.LATE;
                presentCount++;
            } else {
                status = Attendance.AttendanceStatus.ABSENT;
            }
            attendanceRepository.save(Attendance.builder()
                    .student(student)
                    .subjectOffering(offering)
                    .session(session)
                    .date(session.getClassDate())
                    .status(status)
                    .reason(status == Attendance.AttendanceStatus.ABSENT ? "No reason provided" : null)
                    .build());
        }
        return roundOne((double) presentCount / sessions.size());
    }

    private void seedNews(SeedContext context) {
        List<News> news = List.of(
                news("Spring registration window is open", "ACADEMIC", "Students can review their current schedules, journal, and active registration period through the portal.", context.now.minusSeconds(86400L * 10)),
                news("Digital entrepreneurship showcase", "EVENT", "SITE and Business School teams will present product demos and analytics dashboards in the innovation hall.", context.now.minusSeconds(86400L * 8)),
                news("Exam timetable publication reminder", "REGISTRAR", "Draft exam schedules for the spring term are available for review before final publication.", context.now.minusSeconds(86400L * 6)),
                news("Career center internship briefing", "CAREER", "Fourth-year students can review internship expectations, reports, and supervisor contact details.", context.now.minusSeconds(86400L * 4)),
                news("Mobility applications open for next academic year", "MOBILITY", "Students interested in exchange programs can submit requests with supporting documents through the portal.", context.now.minusSeconds(86400L * 2)),
                news("Student support week", "CAMPUS", "Support, registrar, and finance offices are extending consultation hours this week for portal users.", context.now.minusSeconds(43200))
        );
        newsRepository.saveAll(news);
    }

    private News news(String title, String category, String content, Instant createdAt) {
        return News.builder()
                .title(title)
                .category(category)
                .content(content)
                .createdAt(createdAt)
                .build();
    }

    private void seedRequests(SeedContext context) {
        List<RequestTemplate> templates = List.of(
                new RequestTemplate(2, "REGISTRAR", "Need an official enrollment letter for visa extension.", StudentRequest.RequestStatus.DONE, "registrar@kbtu.kz", "The signed letter is ready for download in the student cabinet."),
                new RequestTemplate(9, "ACADEMIC", "Please review a low score in Attestation 1 for Operating Systems.", StudentRequest.RequestStatus.IN_REVIEW, "support@kbtu.kz", "The course instructor has been notified and will respond within two business days."),
                new RequestTemplate(14, "MOBILITY", "Would like to discuss exchange options for the next fall semester.", StudentRequest.RequestStatus.NEW, "mobility@kbtu.kz", "Your request is queued for the mobility office."),
                new RequestTemplate(21, "FINANCE", "Need a confirmation that spring tuition is fully paid.", StudentRequest.RequestStatus.APPROVED, "finance@kbtu.kz", "Payment confirmation has been attached to your profile."),
                new RequestTemplate(33, "REGISTRAR", "Please update my phone number in the system.", StudentRequest.RequestStatus.NEED_INFO, "registrar@kbtu.kz", "Please upload a short confirmation note with the new contact details."),
                new RequestTemplate(41, "ACADEMIC", "Need guidance on add/drop for an overloaded semester.", StudentRequest.RequestStatus.DONE, "support@kbtu.kz", "Advisor recommendation has been added to your record."),
                new RequestTemplate(56, "DOCUMENTS", "Requesting an unofficial transcript for internship submission.", StudentRequest.RequestStatus.DONE, "registrar@kbtu.kz", "The transcript PDF is now available in your requests panel."),
                new RequestTemplate(67, "GENERAL", "Need clarification about survey deadline and completion rules.", StudentRequest.RequestStatus.APPROVED, "support@kbtu.kz", "The survey remains open until the end of the month and can only be submitted once."),
                new RequestTemplate(78, "MOBILITY", "Question about course mapping for a partner university.", StudentRequest.RequestStatus.IN_REVIEW, "mobility@kbtu.kz", "The mapping review is currently in progress."),
                new RequestTemplate(85, "FINANCE", "Please confirm there are no active financial holds on my account.", StudentRequest.RequestStatus.DONE, "finance@kbtu.kz", "There are no active holds on your account."),
                new RequestTemplate(92, "REGISTRAR", "Need support with a late schedule update after section change.", StudentRequest.RequestStatus.APPROVED, "registrar@kbtu.kz", "Your updated schedule is already reflected in the portal."),
                new RequestTemplate(99, "ACADEMIC", "Would like a consultation about low attendance warning.", StudentRequest.RequestStatus.NEW, "support@kbtu.kz", "Support desk has received the request.")
        );

        int index = 0;
        for (RequestTemplate template : templates) {
            Student student = context.students.get(template.studentIndex() - 1);
            User assignee = context.usersByEmail.get(template.assigneeEmail());
            Instant createdAt = context.now.minusSeconds(86400L * (index + 1));
            StudentRequest request = studentRequestRepository.save(StudentRequest.builder()
                    .student(student)
                    .category(template.category())
                    .description(template.description())
                    .status(template.status())
                    .assignedTo(assignee)
                    .createdAt(createdAt)
                    .updatedAt(createdAt.plusSeconds(7200))
                    .closedAt(template.status() == StudentRequest.RequestStatus.DONE || template.status() == StudentRequest.RequestStatus.APPROVED
                            ? createdAt.plusSeconds(172800)
                            : null)
                    .build());

            requestMessageRepository.save(RequestMessage.builder()
                    .request(request)
                    .sender(context.usersByEmail.get(student.getEmail()))
                    .message(template.description())
                    .createdAt(createdAt)
                    .build());
            requestMessageRepository.save(RequestMessage.builder()
                    .request(request)
                    .sender(assignee)
                    .message(template.response())
                    .createdAt(createdAt.plusSeconds(3600))
                    .build());

            context.notifications.add(Notification.builder()
                    .recipientEmail(student.getEmail())
                    .title("Request updated")
                    .message("Your " + template.category() + " request is now " + template.status() + ".")
                    .link("/app/student/requests")
                    .type(Notification.NotificationType.REQUEST)
                    .read(false)
                    .createdAt(createdAt.plusSeconds(3600))
                    .build());
            index++;
        }
    }

    private void seedSurvey(SeedContext context) {
        Survey survey = Survey.builder()
                .title("Spring 2025-2026 Student Experience Survey")
                .startDate(context.today.minusDays(5))
                .endDate(context.today.plusDays(20))
                .anonymous(false)
                .semester(context.currentSemester())
                .build();

        List<SurveyQuestion> questions = new ArrayList<>();
        questions.add(SurveyQuestion.builder()
                .survey(survey)
                .type(SurveyQuestion.QuestionType.SCALE)
                .text("How clear is the current semester schedule in the portal?")
                .build());
        questions.add(SurveyQuestion.builder()
                .survey(survey)
                .type(SurveyQuestion.QuestionType.TEXT)
                .text("What improvement would you like to see next in the student portal?")
                .build());
        survey.setQuestions(questions);
        Survey savedSurvey = surveyRepository.save(survey);

        for (int i = 0; i < 35; i++) {
            Student student = context.students.get(i);
            surveyResponseRepository.save(SurveyResponse.builder()
                    .survey(savedSurvey)
                    .student(student)
                    .answersJson("{\"q1\":5,\"q2\":\"Need more mobile-first workflows and clearer notifications.\"}")
                    .submittedAt(context.now.minusSeconds(6000L + i * 173L))
                    .build());
        }
    }

    private void seedChecklists(SeedContext context) {
        checklistTemplateRepository.saveAll(List.of(
                ChecklistTemplate.builder()
                        .title("Review spring course schedule")
                        .linkToSection("/app/student/schedule")
                        .triggerEvent(ChecklistTemplate.TriggerEvent.SEMESTER_START)
                        .offsetDays(0)
                        .active(true)
                        .build(),
                ChecklistTemplate.builder()
                        .title("Confirm paid tuition receipt")
                        .linkToSection("/app/student/financial")
                        .triggerEvent(ChecklistTemplate.TriggerEvent.SEMESTER_START)
                        .offsetDays(3)
                        .active(true)
                        .build(),
                ChecklistTemplate.builder()
                        .title("Complete student feedback survey")
                        .linkToSection("/app/student/surveys")
                        .triggerEvent(ChecklistTemplate.TriggerEvent.SEMESTER_END)
                        .offsetDays(-15)
                        .active(true)
                        .build()
        ));

        for (Student student : context.students) {
            checklistItemRepository.saveAll(List.of(
                    ChecklistItem.builder()
                            .student(student)
                            .title("Review spring course schedule")
                            .deadline(context.currentSemester().getStartDate().plusDays(3))
                            .completed(true)
                            .linkToSection("/app/student/schedule")
                            .build(),
                    ChecklistItem.builder()
                            .student(student)
                            .title("Confirm paid tuition receipt")
                            .deadline(context.currentSemester().getStartDate().plusDays(7))
                            .completed(true)
                            .linkToSection("/app/student/financial")
                            .build(),
                    ChecklistItem.builder()
                            .student(student)
                            .title("Complete student feedback survey")
                            .deadline(context.currentSemester().getEndDate().minusDays(10))
                            .completed(student.getId() % 3 == 0)
                            .linkToSection("/app/student/surveys")
                            .build()
            ));
        }
    }
    private void seedCurrentCourseArtifacts(SeedContext context) {
        List<SubjectOffering> currentOfferings = context.offerings.values().stream()
                .filter(offering -> offering.getSemester().isCurrent())
                .sorted(Comparator.comparing(offering -> offering.getSubject().getCode()))
                .toList();

        for (SubjectOffering offering : currentOfferings) {
            courseAnnouncementRepository.save(CourseAnnouncement.builder()
                    .teacher(offering.getTeacher())
                    .subjectOffering(offering)
                    .title(offering.getSubject().getCode() + " weekly update")
                    .content("Lecture slides, practice guidance, and assessment reminders are available for this section.")
                    .publicVisible(true)
                    .published(true)
                    .pinned(Math.floorMod(offering.getSubject().getCode().hashCode(), 5) == 0)
                    .scheduledAt(null)
                    .publishedAt(context.now.minusSeconds(Math.floorMod(offering.getSubject().getCode().hashCode(), 7200)))
                    .createdAt(context.now.minusSeconds(Math.floorMod(offering.getSubject().getCode().hashCode(), 86400)))
                    .updatedAt(context.now.minusSeconds(Math.floorMod(offering.getSubject().getCode().hashCode(), 3600)))
                    .build());

            List<Registration> registrations = context.currentRegistrationsByOffering.getOrDefault(offering.getId(), List.of());
            for (Registration registration : registrations) {
                String key = studentOfferingKey(registration.getStudent().getId(), offering.getId());
                double coursework = context.currentCourseworkTotals.getOrDefault(key, 60.0);
                double attendanceRatio = context.attendanceRatios.getOrDefault(key, 1.0);
                if (coursework < 34 || attendanceRatio < 0.74) {
                    TeacherStudentNote.RiskFlag riskFlag = coursework < 34 && attendanceRatio < 0.74
                            ? TeacherStudentNote.RiskFlag.COMBINED_RISK
                            : coursework < 34 ? TeacherStudentNote.RiskFlag.LOW_GRADES : TeacherStudentNote.RiskFlag.LOW_ATTENDANCE;
                    teacherStudentNoteRepository.save(TeacherStudentNote.builder()
                            .teacher(offering.getTeacher())
                            .student(registration.getStudent())
                            .subjectOffering(offering)
                            .note(riskFlag == TeacherStudentNote.RiskFlag.LOW_GRADES
                                    ? "Student needs support before the next attestation."
                                    : riskFlag == TeacherStudentNote.RiskFlag.LOW_ATTENDANCE
                                    ? "Attendance has dropped below the recommended threshold."
                                    : "Combined grade and attendance risk. Recommend consultation this week.")
                            .riskFlag(riskFlag)
                            .createdAt(context.now.minusSeconds(Math.floorMod(Objects.hash(key, "note"), 200000)))
                            .updatedAt(context.now.minusSeconds(Math.floorMod(Objects.hash(key, "note-update"), 90000)))
                            .build());
                }
            }
        }

        List<SubjectOffering> gradeChangeTargets = currentOfferings.stream().limit(6).toList();
        User registrar = context.usersByEmail.get("registrar@kbtu.kz");
        for (int i = 0; i < gradeChangeTargets.size(); i++) {
            SubjectOffering offering = gradeChangeTargets.get(i);
            List<Registration> registrations = context.currentRegistrationsByOffering.getOrDefault(offering.getId(), List.of());
            if (registrations.isEmpty()) {
                continue;
            }
            Student student = registrations.get(0).getStudent();
            Grade currentGrade = gradeRepository.findBySubjectOfferingIdWithDetails(offering.getId()).stream()
                    .filter(grade -> grade.getStudent().getId().equals(student.getId()))
                    .max(Comparator.comparing(grade -> grade.getComponent().getCreatedAt()))
                    .orElse(null);
            if (currentGrade == null) {
                continue;
            }
            double oldValue = currentGrade.getGradeValue();
            GradeChangeRequest.RequestStatus status = switch (i % 3) {
                case 0 -> GradeChangeRequest.RequestStatus.SUBMITTED;
                case 1 -> GradeChangeRequest.RequestStatus.APPROVED;
                default -> GradeChangeRequest.RequestStatus.APPLIED;
            };
            double newValue = clamp(roundOne(oldValue + 2.0), 0, currentGrade.getMaxGradeValue());
            if (status == GradeChangeRequest.RequestStatus.APPLIED) {
                currentGrade.setGradeValue(newValue);
                currentGrade.setComment("Adjusted after review");
                gradeRepository.save(currentGrade);
            }
            gradeChangeRequestRepository.save(GradeChangeRequest.builder()
                    .teacher(offering.getTeacher())
                    .student(student)
                    .subjectOffering(offering)
                    .grade(currentGrade)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .reason("Manual review after rechecking rubric alignment.")
                    .status(status)
                    .reviewerComment(status == GradeChangeRequest.RequestStatus.SUBMITTED ? null : "Reviewed by registrar office.")
                    .reviewedBy(status == GradeChangeRequest.RequestStatus.SUBMITTED ? null : registrar)
                    .createdAt(context.now.minusSeconds(86400L * (i + 1)))
                    .reviewedAt(status == GradeChangeRequest.RequestStatus.SUBMITTED ? null : context.now.minusSeconds(43200L * (i + 1)))
                    .appliedAt(status == GradeChangeRequest.RequestStatus.APPLIED ? context.now.minusSeconds(21600L * (i + 1)) : null)
                    .build());
        }
    }

    private void seedFinance(SeedContext context) {
        for (Student student : context.students) {
            BigDecimal amount = BigDecimal.valueOf(345000L + student.getCourse() * 12000L);
            Charge charge = chargeRepository.save(Charge.builder()
                    .student(student)
                    .amount(amount)
                    .description(context.currentSemester().getName() + " tuition invoice")
                    .dueDate(context.currentSemester().getStartDate().plusDays(10))
                    .status(Charge.ChargeStatus.PAID)
                    .build());
            paymentRepository.save(Payment.builder()
                    .student(student)
                    .amount(amount)
                    .date(context.currentSemester().getStartDate().plusDays(5))
                    .charge(charge)
                    .build());
        }
    }

    private void seedNotifications(SeedContext context) {
        for (Student student : context.students) {
            context.notifications.add(Notification.builder()
                    .recipientEmail(student.getEmail())
                    .title("Spring schedule confirmed")
                    .message("Your current semester schedule and registrations are active in the portal.")
                    .link("/app/student/schedule")
                    .type(Notification.NotificationType.ENROLLMENT)
                    .read(false)
                    .createdAt(context.now.minusSeconds(Math.floorMod(Objects.hash(student.getEmail(), "schedule"), 200000)))
                    .build());
            context.notifications.add(Notification.builder()
                    .recipientEmail(student.getEmail())
                    .title("Financial account updated")
                    .message("A paid tuition invoice is visible in your financial cabinet.")
                    .link("/app/student/financial")
                    .type(Notification.NotificationType.FINANCE)
                    .read(student.getId() % 4 == 0)
                    .createdAt(context.now.minusSeconds(Math.floorMod(Objects.hash(student.getEmail(), "finance"), 200000)))
                    .build());
        }

        for (Teacher teacher : allTeachers(context)) {
            context.notifications.add(Notification.builder()
                    .recipientEmail(teacher.getEmail())
                    .title("Section roster updated")
                    .message("Your current semester sections now include fresh attendance and journal data.")
                    .link("/app/professor")
                    .type(Notification.NotificationType.SYSTEM)
                    .read(false)
                    .createdAt(context.now.minusSeconds(Math.floorMod(Objects.hash(teacher.getEmail(), "roster"), 150000)))
                    .build());
        }

        for (String adminEmail : List.of("admin@kbtu.kz", "registrar@kbtu.kz", "finance@kbtu.kz", "support@kbtu.kz", "mobility@kbtu.kz")) {
            context.notifications.add(Notification.builder()
                    .recipientEmail(adminEmail)
                    .title("Demo dataset refreshed")
                    .message("Portal demo data now includes 100 students, realistic schedules, grades, requests, and finance records.")
                    .link("/app/admin")
                    .type(Notification.NotificationType.SYSTEM)
                    .read(false)
                    .createdAt(context.now)
                    .build());
        }

        notificationRepository.saveAll(context.notifications);
    }

    private Teacher pickTeacher(SeedContext context, String subjectCode) {
        String facultyName = context.subjectHomeFaculty.get(subjectCode);
        List<Teacher> teachers = context.teachersByFaculty.getOrDefault(facultyName, allTeachers(context));
        return teachers.get(Math.floorMod(subjectCode.hashCode(), teachers.size()));
    }

    private List<Teacher> allTeachers(SeedContext context) {
        return context.teachersByFaculty.values().stream().flatMap(List::stream).toList();
    }

    private List<MeetingSlotSeed> buildMeetingPattern(Subject subject) {
        String subjectCode = subject.getCode();
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
        LocalTime[] starts = {LocalTime.of(8, 0), LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(14, 0), LocalTime.of(16, 0), LocalTime.of(18, 0)};
        int hash = Math.floorMod(subjectCode.hashCode(), 10_000);
        if (isPracticeOnlySubject(subject)) {
            int startIndex = (hash / 11) % starts.length;
            int dayIndex = hash % days.length;
            List<MeetingSlotSeed> slots = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                DayOfWeek day = days[(dayIndex + i) % days.length];
                LocalTime start = starts[(startIndex + i) % starts.length];
                slots.add(new MeetingSlotSeed(
                        day,
                        start,
                        start.plusHours(1),
                        roomCode(subjectCode, "P"),
                        SubjectOffering.LessonType.PRACTICE
                ));
            }
            return slots;
        }

        DayOfWeek lectureDay = days[hash % days.length];
        LocalTime lectureStart = starts[(hash / 7) % starts.length];
        DayOfWeek practiceDay = days[(hash + 2) % days.length];
        LocalTime practiceStart = starts[(hash / 13) % starts.length];

        return List.of(
                new MeetingSlotSeed(
                        lectureDay,
                        lectureStart,
                        lectureStart.plusHours(2),
                        roomCode(subjectCode, "L"),
                        SubjectOffering.LessonType.LECTURE
                ),
                new MeetingSlotSeed(
                        practiceDay,
                        practiceStart,
                        practiceStart.plusHours(1),
                        roomCode(subjectCode, "P"),
                        SubjectOffering.LessonType.PRACTICE
                )
        );
    }

    private boolean isPracticeOnlySubject(Subject subject) {
        String code = subject.getCode();
        String name = subject.getName() != null ? subject.getName().toLowerCase(Locale.ROOT) : "";
        return code.startsWith("LAN")
                || code.startsWith("HUM")
                || code.startsWith("PHE")
                || name.contains("language")
                || name.contains("communication")
                || name.contains("history")
                || name.contains("philosophy");
    }

    private String roomCode(String subjectCode, String prefix) {
        return prefix + "-" + (200 + Math.floorMod(subjectCode.hashCode(), 250));
    }

    private double scoreTarget(SeedContext context, Student student, String subjectCode) {
        double base = context.studentAbility.getOrDefault(student.getId(), 72.0);
        double subjectVariation = centeredVariation(student.getEmail(), subjectCode, 9.0);
        double courseBonus = student.getCourse() * 1.5;
        return clamp(roundOne(base + subjectVariation + courseBonus), 48, 97);
    }

    private double centeredVariation(String seedA, String seedB, double amplitude) {
        int bucket = Math.floorMod(Objects.hash(seedA, seedB), 1000);
        return ((bucket / 999.0) - 0.5) * amplitude * 2.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private GradeScale gradeScale(double numeric) {
        if (numeric >= 95) return new GradeScale("A", 4.0);
        if (numeric >= 90) return new GradeScale("A-", 3.67);
        if (numeric >= 85) return new GradeScale("B+", 3.33);
        if (numeric >= 80) return new GradeScale("B", 3.0);
        if (numeric >= 75) return new GradeScale("B-", 2.67);
        if (numeric >= 70) return new GradeScale("C+", 2.33);
        if (numeric >= 65) return new GradeScale("C", 2.0);
        if (numeric >= 60) return new GradeScale("C-", 1.67);
        if (numeric >= 55) return new GradeScale("D+", 1.33);
        if (numeric >= 50) return new GradeScale("D", 1.0);
        return new GradeScale("F", 0.0);
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private String studentOfferingKey(Long studentId, Long offeringId) {
        return studentId + "|" + offeringId;
    }

    private void writeCredentialsFile(List<AccountCredential> credentials) {
        Map<String, List<AccountCredential>> grouped = new LinkedHashMap<>();
        for (AccountCredential credential : credentials) {
            grouped.computeIfAbsent(credential.role(), key -> new ArrayList<>()).add(credential);
        }

        StringBuilder builder = new StringBuilder();
        builder.append("KBTU Portal demo accounts").append(System.lineSeparator())
                .append("Generated: ").append(Instant.now()).append(System.lineSeparator()).append(System.lineSeparator());
        for (Map.Entry<String, List<AccountCredential>> entry : grouped.entrySet()) {
            builder.append("[").append(entry.getKey()).append("]").append(System.lineSeparator());
            for (AccountCredential credential : entry.getValue()) {
                builder.append(credential.email())
                        .append(" | ")
                        .append(credential.password())
                        .append(" | ")
                        .append(credential.fullName())
                        .append(" | ")
                        .append(credential.details())
                        .append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
        }

        Path file = Path.of(LOCAL_CREDENTIALS_FILE);
        try {
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write local seed credentials file", exception);
        }
    }

    private List<AccountCredential> buildKnownCredentials() {
        List<AccountCredential> credentials = new ArrayList<>();
        credentials.add(new AccountCredential("ADMIN", "admin@kbtu.kz", ADMIN_PASSWORD, "System Administrator", "[SUPER, REGISTRAR, FINANCE, MOBILITY, SUPPORT, CONTENT]"));
        credentials.add(new AccountCredential("ADMIN", "registrar@kbtu.kz", ADMIN_PASSWORD, "Registrar Office", "[REGISTRAR]"));
        credentials.add(new AccountCredential("ADMIN", "finance@kbtu.kz", ADMIN_PASSWORD, "Finance Office", "[FINANCE]"));
        credentials.add(new AccountCredential("ADMIN", "support@kbtu.kz", ADMIN_PASSWORD, "Student Support Office", "[SUPPORT, CONTENT]"));
        credentials.add(new AccountCredential("ADMIN", "mobility@kbtu.kz", ADMIN_PASSWORD, "Mobility Office", "[MOBILITY]"));
        for (TeacherSeed seed : teacherSeeds()) {
            credentials.add(new AccountCredential("PROFESSOR", DemoIdentitySupport.teacherEmailFromFullName(seed.name()), PROFESSOR_PASSWORD, seed.name(), seed.facultyName()));
        }
        for (int i = 1; i <= STUDENT_COUNT; i++) {
            String fullName = DemoIdentitySupport.generateStudentName(i);
            credentials.add(new AccountCredential("STUDENT", DemoIdentitySupport.studentEmailFromFullName(fullName), STUDENT_PASSWORD, fullName, "Demo seeded student"));
        }
        return credentials;
    }
    private String facultyProgramName(String facultyName) {
        return switch (facultyName) {
            case SITE -> "Information Technology and Engineering";
            case SEOGI -> "Energy and Oil and Gas Industry";
            case ISE -> "Economics and Analytics";
            case BUSINESS -> "Business and Management";
            case KMA -> "Maritime Engineering";
            case SMC -> "Mathematics and Cybernetics";
            case GEO -> "Geology and Earth Data";
            case MATERIALS -> "Materials Science and Green Technologies";
            default -> facultyName;
        };
    }

    private String departmentForFaculty(String facultyName) {
        return switch (facultyName) {
            case SITE -> "Department of Digital Systems";
            case SEOGI -> "Department of Energy Systems";
            case ISE -> "Department of Economics and Data Analysis";
            case BUSINESS -> "Department of Business Practice";
            case KMA -> "Department of Maritime Studies";
            case SMC -> "Department of Applied Mathematics";
            case GEO -> "Department of Geology";
            case MATERIALS -> "Department of Materials and Sustainable Engineering";
            default -> "Academic Department";
        };
    }

    private String roomForFaculty(String facultyName, int index) {
        String prefix = switch (facultyName) {
            case SITE -> "SITE";
            case SEOGI -> "ENG";
            case ISE -> "ISE";
            case BUSINESS -> "BUS";
            case KMA -> "KMA";
            case SMC -> "SMC";
            case GEO -> "GEO";
            case MATERIALS -> "MAT";
            default -> "KBTU";
        };
        return prefix + "-" + (310 + index);
    }

    private String inferHomeFaculty(String code, String name) {
        if (Set.of("PET1272", "PET1239", "PET1240").contains(code)) {
            return GEO;
        }
        if (Set.of("INFT4152", "INFT1301", "MAT1322", "INFG3233").contains(code)) {
            return MATERIALS;
        }
        if (code.startsWith("PET") || code.startsWith("CEEN") || code.startsWith("ACEN")) {
            return SEOGI;
        }
        if (code.startsWith("FIN") || code.startsWith("MNG")) {
            return BUSINESS;
        }
        if (code.startsWith("ISE") || code.startsWith("ECO") || code.startsWith("STAT")) {
            return ISE;
        }
        if (code.startsWith("KMA") || code.startsWith("ITMA")) {
            return KMA;
        }
        if (code.startsWith("CHE") || code.startsWith("FUN")) {
            return MATERIALS;
        }
        if (code.startsWith("MAT") || code.startsWith("MATH")) {
            return SMC;
        }
        if (code.startsWith("LAN") || code.startsWith("HUM") || code.startsWith("PHE")) {
            return SMC;
        }
        return SITE;
    }

    private int defaultCredits(String code, String name) {
        if (code.startsWith("PHE")) {
            return 2;
        }
        if (code.startsWith("PRA")) {
            return 6;
        }
        if (code.startsWith("LAN") || code.startsWith("HUM")) {
            return 3;
        }
        if (code.startsWith("FIN") || code.startsWith("MNG") || code.startsWith("ECO") || code.startsWith("STAT") || code.startsWith("ISE")) {
            return 3;
        }
        if (name.contains("English") || name.contains("Language") || name.contains("Communication")) {
            return 3;
        }
        return 4;
    }

    private List<String> facultyNames() {
        return List.of(SITE, SEOGI, ISE, BUSINESS, KMA, SMC, GEO, MATERIALS);
    }

    private List<TeacherSeed> teacherSeeds() {
        return List.of(
                new TeacherSeed("Professor Aidos Nurgaliyev", SITE),
                new TeacherSeed("Professor Madina Tolegenova", ISE),
                new TeacherSeed("Professor Yerlan Sadykov", SEOGI),
                new TeacherSeed("Professor Dana Zhaksylykova", BUSINESS),
                new TeacherSeed("Professor Arman Beketov", SMC),
                new TeacherSeed("Professor Aigerim Kassenova", MATERIALS),
                new TeacherSeed("Professor Nurbolat Akhmetov", GEO),
                new TeacherSeed("Professor Aliya Tursynbekova", KMA),
                new TeacherSeed("Professor Rustam Serikbayev", SITE),
                new TeacherSeed("Professor Zhanar Baimukhanova", BUSINESS)
        );
    }

    private List<SubjectSeed> subjectCatalog() {
        return List.of(
                new SubjectSeed("INF3230", "IT Audit"),
                new SubjectSeed("INFT3108", "IT Project Management"),
                new SubjectSeed("PRA334", "Industrial Internship"),
                new SubjectSeed("INFT3140", "JS Framework. Angular"),
                new SubjectSeed("INFT3139", "JS Framework. React"),
                new SubjectSeed("CSCI4236", "Power BI business analysis and data visualization"),
                new SubjectSeed("INFS1202", "Responsible Digital Entrepreneurship"),
                new SubjectSeed("INFT3134", "Advanced Django"),
                new SubjectSeed("INFT3137", "Android Advanced"),
                new SubjectSeed("INFT3105", "Cyber Security Fundamentals"),
                new SubjectSeed("INFT3210", "Field Projects for Information Systems"),
                new SubjectSeed("CSCI3110", "Operating Systems"),
                new SubjectSeed("HUM1102", "Philosophy"),
                new SubjectSeed("INFT3131", "Backend Framework. Django"),
                new SubjectSeed("INFT3107", "Basics of Information Systems"),
                new SubjectSeed("LAN1153", "Cross Cultural Communication"),
                new SubjectSeed("INFT3106", "Fundamentals of Business for Information Systems"),
                new SubjectSeed("HUM1101", "History of Kazakhstan"),
                new SubjectSeed("CSCI2208", "Software Engineering"),
                new SubjectSeed("INFT3135", "Android Development"),
                new SubjectSeed("INFT2102", "IT infrastructure and Computer Networks"),
                new SubjectSeed("INFT2204", "Introduction to Business Management"),
                new SubjectSeed("PHE102", "Physical Education II"),
                new SubjectSeed("LAN1118", "Professional Kazakh Language (Rukhani Zhangyru)"),
                new SubjectSeed("INFT2205", "Web Development"),
                new SubjectSeed("CSCI2105", "Algorithms and Data Structures"),
                new SubjectSeed("CSCI2104", "Databases"),
                new SubjectSeed("INFT1101", "Information and Communication Technologies"),
                new SubjectSeed("CSCI2106", "Object-Oriented Programming and Design"),
                new SubjectSeed("PHE101", "Physical Education I"),
                new SubjectSeed("LAN1119", "Russian Language"),
                new SubjectSeed("MATH1202", "Calculus II"),
                new SubjectSeed("HUM1137", "Merging Personal and Global Evolution"),
                new SubjectSeed("FUN1105", "Physics I"),
                new SubjectSeed("CSCI1204", "Programming Principles II"),
                new SubjectSeed("STAT2201", "Statistics"),
                new SubjectSeed("MATH1102", "Calculus I"),
                new SubjectSeed("CSCI1102", "Discrete Structures"),
                new SubjectSeed("LAN1182", "English Pre-Intermediate (A2)"),
                new SubjectSeed("MATH1203", "Linear Algebra for Engineers"),
                new SubjectSeed("CSCI1101", "Programming Principles I"),
                new SubjectSeed("MAT1322", "3D Modeling"),
                new SubjectSeed("INFT3143", "Backend Framework: Laravel"),
                new SubjectSeed("INFT3132", "Backend Framework: Spring"),
                new SubjectSeed("INFS4145", "DevOps II"),
                new SubjectSeed("CSE1362", "ICPC ACM Competitive Programming II"),
                new SubjectSeed("CSE1366", "ICPC Competitive Programming I"),
                new SubjectSeed("ISE1325", "International Economics II"),
                new SubjectSeed("MNG1331", "Python for Business Analytics"),
                new SubjectSeed("ACEN4236", "SCADA Systems and Industrial Networks"),
                new SubjectSeed("CEEN1304", "SCADA Solutions for Industry"),
                new SubjectSeed("INFT4152", "SolidWorks"),
                new SubjectSeed("INFT4244", "UI/UX Design"),
                new SubjectSeed("INFG3233", "VFX and 3D Physics"),
                new SubjectSeed("INFT2203", "Web Development"),
                new SubjectSeed("ISE1307", "Abstract Mathematics II"),
                new SubjectSeed("CEEN3313", "Automation of Standard Technological Processes"),
                new SubjectSeed("CEEN1301", "Automated Control Systems Based on SIMATIC"),
                new SubjectSeed("PET1272", "Analysis of Oil and Gas Projects"),
                new SubjectSeed("FIN1327", "Fraud Principles Analysis"),
                new SubjectSeed("CHE1225a", "Analytical Chemistry"),
                new SubjectSeed("CHE1225", "Analytical Chemistry"),
                new SubjectSeed("LAN1184", "English Upper-Intermediate (B2) (Coursera)"),
                new SubjectSeed("ISE1104", "English Language II"),
                new SubjectSeed("INFT4139", "System Architecture"),
                new SubjectSeed("FIN1203", "Audit"),
                new SubjectSeed("CSE398", "Databases"),
                new SubjectSeed("INFS4137", "Operating Systems Security"),
                new SubjectSeed("INFS3233", "Network Security"),
                new SubjectSeed("LAN1132", "Business Kazakh Language (B2)"),
                new SubjectSeed("ECO1202", "Business Statistics"),
                new SubjectSeed("STAT1210", "Business Statistics"),
                new SubjectSeed("ISE1240", "Business and Management in Global Context II (6 ECTS)"),
                new SubjectSeed("LAN1109", "Business Negotiations and Business Correspondence (B2)"),
                new SubjectSeed("ISE1331", "Business Analytics, Applied Modeling and Forecasting"),
                new SubjectSeed("MNG1205", "Business Planning and Consulting"),
                new SubjectSeed("FIN1319", "Accounting II (Intermediate Level)"),
                new SubjectSeed("MAT1421", "Variants of O-Minimality"),
                new SubjectSeed("ISE1212", "Introduction to Statistics II"),
                new SubjectSeed("ECO1215", "Introduction to Excel"),
                new SubjectSeed("INFT1301", "Introduction to Fusion 360"),
                new SubjectSeed("INFT4153", "Introduction to SAP ABAP"),
                new SubjectSeed("ISE1210", "Introduction to Macroeconomics"),
                new SubjectSeed("INFS1201", "Introduction to Open Source"),
                new SubjectSeed("FIN1311", "Introduction to Algorithmic Trading"),
                new SubjectSeed("FIN1202", "Introduction to Accounting"),
                new SubjectSeed("MNG1338", "Introduction to Contract Law"),
                new SubjectSeed("CSE1126", "Introduction to Game Design and Development"),
                new SubjectSeed("KMA1242", "Introduction to Engineering"),
                new SubjectSeed("MNG1337", "Introduction to AI Tools in Project Management"),
                new SubjectSeed("ISE1214", "Introduction to Calculus II"),
                new SubjectSeed("CSCI3240", "Introduction to Computer Vision"),
                new SubjectSeed("ITMA1201", "Introduction to Computer Networks"),
                new SubjectSeed("CSCI2107", "Introduction to Computer Networks"),
                new SubjectSeed("MNG1202", "Introduction to Marketing"),
                new SubjectSeed("CSCI3237", "Introduction to Machine Learning"),
                new SubjectSeed("INFJ2101", "Introduction to Media"),
                new SubjectSeed("CEEN3107", "Introduction to Microcontrollers and Microprocessor Systems"),
                new SubjectSeed("MAT1309", "Mathematical Foundations of Cryptography"),
                new SubjectSeed("MATH2207", "Mathematical Foundations of Information Security"),
                new SubjectSeed("MAT1232", "Mathematical Engineering in Economics II"),
                new SubjectSeed("MAT1237", "Complex Analysis"),
                new SubjectSeed("MAT1227", "Mathematical Modeling of Chemical Processes"),
                new SubjectSeed("CSCI3234", "Machine Learning"),
                new SubjectSeed("ISE1321", "Machine Learning I"),
                new SubjectSeed("ISE1322", "Machine Learning II"),
                new SubjectSeed("PET1270", "Machine Learning in Petroleum Engineering"),
                new SubjectSeed("PET1239", "Machine Learning in Oil Exploration and Production"),
                new SubjectSeed("KMA1303", "Medical Care"),
                new SubjectSeed("ECO1207", "International Economics and Trade"),
                new SubjectSeed("MNG1304", "International Business"),
                new SubjectSeed("MNG1224", "Small Business Management"),
                new SubjectSeed("ITMA3008", "Information Systems Management"),
                new SubjectSeed("ISE1354", "Information Systems Management II"),
                new SubjectSeed("PET1240", "Finite Element Method in Engineering"),
                new SubjectSeed("FUN1228", "Materials Selection Methodology"),
                new SubjectSeed("MNG1204", "Business Research Methods"),
                new SubjectSeed("PET1312", "Enhanced Oil Recovery Methods"),
                new SubjectSeed("PET1405", "Enhanced Oil Recovery Methods")
        );
    }


    private Map<String, List<List<String>>> buildCurriculum() {
        Map<String, List<List<String>>> curriculum = new LinkedHashMap<>();
        curriculum.put(SITE, List.of(
                List.of("INFT1101", "CSCI1101", "CSCI1102", "MATH1102", "LAN1182"),
                List.of("CSCI1204", "CSCI2106", "CSCI2107", "MATH1202", "PHE101"),
                List.of("CSCI2104", "CSCI2105", "INFT2205", "INFT2102", "HUM1101"),
                List.of("CSCI2208", "INFS1201", "INFS1202", "LAN1153", "PHE102"),
                List.of("CSCI3110", "INFT3105", "INFT3106", "INFT3107", "STAT2201"),
                List.of("INFT3131", "INFT3135", "INFT3210", "CSCI3237", "MNG1337"),
                List.of("INFT3132", "INFT3134", "INFT3139", "INFT3140", "CSCI3240"),
                List.of("INFT3108", "INF3230", "INFS3233", "INFS4137", "INFS4145")
        ));
        curriculum.put(SEOGI, List.of(
                List.of("FUN1105", "MATH1102", "CHE1225", "MATH1203", "LAN1182"),
                List.of("CHE1225a", "PET1272", "CEEN1301", "CEEN1304", "PHE101"),
                List.of("CEEN3107", "ACEN4236", "CEEN3313", "PET1240", "FUN1228"),
                List.of("PET1312", "PET1405", "PET1270", "PET1239", "ECO1202"),
                List.of("INFS1201", "CSCI2107", "INFT3108", "MNG1204", "CSCI3237"),
                List.of("CSCI3234", "INFT4139", "INFS3233", "INFS4137", "MNG1337"),
                List.of("INFT3143", "INFT3132", "INFT4153", "CSCI4236", "PRA334"),
                List.of("INF3230", "INFS4145", "INFT4244", "INFT4152", "PET1272")
        ));
        curriculum.put(ISE, List.of(
                List.of("ISE1104", "ISE1210", "FIN1202", "ECO1202", "LAN1109"),
                List.of("ISE1212", "ISE1214", "ECO1215", "FIN1203", "MNG1202"),
                List.of("ISE1240", "ISE1325", "STAT1210", "MNG1205", "LAN1132"),
                List.of("ISE1331", "FIN1319", "FIN1327", "MNG1304", "MNG1224"),
                List.of("MNG1331", "ECO1207", "MNG1204", "MNG1338", "FIN1311"),
                List.of("ISE1321", "ISE1322", "ITMA3008", "ISE1354", "MNG1337"),
                List.of("CSE398", "CSCI3237", "INFT4153", "CSCI3234", "INFT4139"),
                List.of("INFT3143", "INFT3132", "CSCI4236", "INFT4244", "PRA334")
        ));
        curriculum.put(BUSINESS, List.of(
                List.of("FIN1202", "MNG1202", "ECO1202", "ISE1104", "LAN1109"),
                List.of("FIN1203", "ECO1215", "MNG1205", "MNG1304", "LAN1132"),
                List.of("FIN1319", "MNG1224", "MNG1338", "ECO1207", "MNG1204"),
                List.of("FIN1311", "FIN1327", "MNG1331", "ISE1240", "MNG1337"),
                List.of("ITMA3008", "ISE1354", "CSE398", "CSCI2104", "INFT4153"),
                List.of("CSCI3237", "CSCI3234", "INFT4139", "CSCI4236", "INFT3108"),
                List.of("INFT3143", "INFT3132", "INFT4244", "INFS1202", "LAN1153"),
                List.of("INF3230", "PRA334", "INFT3139", "INFT3140", "INFS4145")
        ));
        curriculum.put(KMA, List.of(
                List.of("KMA1242", "KMA1303", "ITMA1201", "MATH1102", "LAN1182"),
                List.of("FUN1105", "CSCI2107", "PHE101", "HUM1101", "LAN1153"),
                List.of("ITMA3008", "MNG1202", "ECO1202", "INFS1201", "FIN1202"),
                List.of("CSE398", "CSCI3110", "INFT3108", "INFT3106", "PHE102"),
                List.of("INFS3233", "INFS4137", "INFT4139", "CSCI3237", "MNG1337"),
                List.of("INFT3143", "INFT3132", "CSCI4236", "INFT4153", "INFT4244"),
                List.of("INF3230", "PRA334", "INFS4145", "INFT3139", "INFT3140"),
                List.of("INFT4152", "PET1240", "PET1272", "PET1312", "PET1405")
        ));
        curriculum.put(SMC, List.of(
                List.of("MATH1102", "MATH1203", "CSCI1101", "CSCI1102", "LAN1182"),
                List.of("MATH1202", "CSCI1204", "ISE1307", "STAT2201", "HUM1102"),
                List.of("CSCI2105", "CSCI2104", "CSE1366", "MAT1237", "MAT1232"),
                List.of("MAT1309", "MATH2207", "CSE1362", "CSCI2107", "HUM1137"),
                List.of("CSCI3237", "CSCI3234", "CSCI3240", "ISE1321", "ISE1322"),
                List.of("MAT1421", "MNG1331", "ECO1202", "INFT4244", "INFG3233"),
                List.of("INFS3233", "INFS4137", "INFT4139", "INFS4145", "MAT1322"),
                List.of("INFT4152", "INFT3132", "INFT3134", "INFT3139", "INFT3140")
        ));
        curriculum.put(GEO, List.of(
                List.of("FUN1105", "CHE1225", "MATH1102", "LAN1182", "HUM1101"),
                List.of("CHE1225a", "PET1272", "ECO1202", "PHE101", "MATH1203"),
                List.of("PET1239", "PET1270", "MAT1227", "PET1240", "MNG1204"),
                List.of("PET1312", "PET1405", "CEEN3107", "ACEN4236", "CSCI2107"),
                List.of("CEEN3313", "CEEN1301", "CEEN1304", "INFT3108", "CSCI3237"),
                List.of("CSCI3234", "INFS1201", "INFS3233", "INFS4137", "INFT4139"),
                List.of("INFT3143", "INFT3132", "CSCI4236", "INFT4152", "PRA334"),
                List.of("INF3230", "INFS4145", "INFT4244", "MNG1337", "ECO1207")
        ));
        curriculum.put(MATERIALS, List.of(
                List.of("CHE1225", "FUN1228", "MATH1102", "LAN1182", "HUM1102"),
                List.of("CHE1225a", "MAT1227", "INFT1301", "PHE101", "ECO1215"),
                List.of("MAT1322", "INFG3233", "INFT4152", "INFT4244", "CSE1126"),
                List.of("CEEN1304", "CEEN3107", "ACEN4236", "CSCI2107", "MNG1204"),
                List.of("PET1240", "INFT4139", "CSCI3237", "CSCI3234", "INFS1201"),
                List.of("INFT3132", "INFT3143", "INFT4153", "INFS3233", "INFS4137"),
                List.of("INFS4145", "CSCI4236", "PRA334", "MNG1337", "ECO1207"),
                List.of("INF3230", "INFT3139", "INFT3140", "PET1312", "PET1405")
        ));
        return curriculum;
    }

    private static class SeedContext {
        private LocalDate today;
        private Instant now;
        private final Map<String, Faculty> faculties = new LinkedHashMap<>();
        private final Map<String, Program> programs = new LinkedHashMap<>();
        private final Map<Integer, Semester> semesters = new LinkedHashMap<>();
        private final Map<String, User> usersByEmail = new LinkedHashMap<>();
        private final Map<String, List<Teacher>> teachersByFaculty = new LinkedHashMap<>();
        private final Map<String, Subject> subjects = new LinkedHashMap<>();
        private final Map<String, String> subjectHomeFaculty = new LinkedHashMap<>();
        private final Map<String, SubjectOffering> offerings = new LinkedHashMap<>();
        private final Map<Long, List<AssessmentComponent>> componentsByOffering = new HashMap<>();
        private final Map<Long, List<AttendanceSession>> attendanceSessionsByOffering = new HashMap<>();
        private final Map<Long, List<Registration>> currentRegistrationsByOffering = new HashMap<>();
        private final Map<Long, Integer> studentCurrentSlot = new HashMap<>();
        private final Map<Long, Double> studentAbility = new HashMap<>();
        private final Map<Long, Double> studentAttendanceBias = new HashMap<>();
        private final Map<String, Double> currentCourseworkTotals = new HashMap<>();
        private final Map<String, Double> attendanceRatios = new HashMap<>();
        private final List<Student> students = new ArrayList<>();
        private final List<AccountCredential> credentials = new ArrayList<>();
        private final List<Notification> notifications = new ArrayList<>();
        private Map<String, List<List<String>>> curriculum;

        private Semester currentSemester() {
            return semesters.get(8);
        }
    }

    private record SemesterSpec(int index, String name, LocalDate start, LocalDate end, boolean current) {}

    private record TeacherSeed(String name, String facultyName) {}

    private record SubjectSeed(String code, String name) {}

    private record MeetingSlotSeed(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            SubjectOffering.LessonType lessonType
    ) {}

    private record GradeScale(String letter, double points) {}

    private record RequestTemplate(int studentIndex, String category, String description,
                                   StudentRequest.RequestStatus status, String assigneeEmail, String response) {}

    private record AccountCredential(String role, String email, String password, String fullName, String details) {}
}

