package ru.kors.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ru.kors.finalproject.repository.FacultyRepository facultyRepository;
    private final ru.kors.finalproject.repository.ProgramRepository programRepository;
    private final ru.kors.finalproject.repository.SemesterRepository semesterRepository;
    private final ru.kors.finalproject.repository.StudentRepository studentRepository;
    private final ru.kors.finalproject.repository.TeacherRepository teacherRepository;
    private final ru.kors.finalproject.repository.SubjectRepository subjectRepository;
    private final ru.kors.finalproject.repository.SubjectOfferingRepository subjectOfferingRepository;
    private final ru.kors.finalproject.repository.SubjectPrerequisiteRepository subjectPrerequisiteRepository;
    private final ru.kors.finalproject.repository.RegistrationRepository registrationRepository;
    private final ru.kors.finalproject.repository.AddDropPeriodRepository addDropPeriodRepository;
    private final ru.kors.finalproject.repository.NewsRepository newsRepository;
    private final ru.kors.finalproject.repository.StudentRequestRepository studentRequestRepository;
    private final ru.kors.finalproject.repository.SurveyRepository surveyRepository;
    private final ru.kors.finalproject.repository.GradeRepository gradeRepository;
    private final ru.kors.finalproject.repository.AttendanceRepository attendanceRepository;
    private final ru.kors.finalproject.repository.ChargeRepository chargeRepository;
    private final ru.kors.finalproject.repository.PaymentRepository paymentRepository;
    private final ru.kors.finalproject.repository.ChecklistItemRepository checklistItemRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (facultyRepository.count() > 0) return;

        Faculty fit = Faculty.builder().name("Faculty of Information Technology").build();
        Faculty sbm = Faculty.builder().name("School of Business Management").build();
        facultyRepository.saveAll(List.of(fit, sbm));

        Semester spring2025 = Semester.builder()
                .name("Spring 2025")
                .startDate(LocalDate.of(2025, 1, 15))
                .endDate(LocalDate.of(2025, 5, 30))
                .current(true)
                .build();
        semesterRepository.save(spring2025);

        Program cs = Program.builder().name("Computer Science").creditLimit(21).faculty(fit).build();
        Program se = Program.builder().name("Software Engineering").creditLimit(21).faculty(fit).build();
        programRepository.saveAll(List.of(cs, se));

        Teacher teacher = Teacher.builder().email("z.teacher@kbtu.kz").name("Dr. Z. Teacher").faculty(fit).build();
        teacherRepository.save(teacher);

        Student student = Student.builder()
                .email("a_mustafayev@kbtu.kz")
                .name("Aidar Mustafayev")
                .course(3)
                .groupName("CS-301")
                .status(Student.StudentStatus.ACTIVE)
                .program(cs)
                .faculty(fit)
                .currentSemester(spring2025)
                .creditsEarned(90)
                .address("Almaty, Kazakhstan")
                .phone("+7 777 123 4567")
                .emergencyContact("Parent: +7 777 987 6543")
                .build();
        studentRepository.save(student);

        Subject math = Subject.builder().code("MATH101").name("Calculus I").credits(4).program(cs).build();
        Subject prog = Subject.builder().code("CS201").name("Programming").credits(4).program(cs).build();
        Subject algo = Subject.builder().code("CS301").name("Algorithms").credits(4).program(cs).build();
        subjectRepository.saveAll(List.of(math, prog, algo));
        subjectPrerequisiteRepository.save(SubjectPrerequisite.builder().subject(algo).prerequisite(prog).build());

        SubjectOffering mathOff = SubjectOffering.builder()
                .subject(math).semester(spring2025).teacher(teacher)
                .capacity(50).dayOfWeek(DayOfWeek.MONDAY).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 30))
                .room("A101").lessonType(SubjectOffering.LessonType.LECTURE).build();
        SubjectOffering progOff = SubjectOffering.builder()
                .subject(prog).semester(spring2025).teacher(teacher)
                .capacity(30).dayOfWeek(DayOfWeek.WEDNESDAY).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(12, 30))
                .room("B205").lessonType(SubjectOffering.LessonType.PRACTICE).build();
        SubjectOffering algoOff = SubjectOffering.builder()
                .subject(algo).semester(spring2025).teacher(teacher)
                .capacity(25).dayOfWeek(DayOfWeek.TUESDAY).startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(15, 30))
                .room("B210").lessonType(SubjectOffering.LessonType.LECTURE).build();
        subjectOfferingRepository.saveAll(List.of(mathOff, progOff, algoOff));

        AddDropPeriod addDrop = AddDropPeriod.builder()
                .semester(spring2025)
                .addStart(LocalDate.of(2025, 1, 10))
                .addEnd(LocalDate.of(2025, 2, 15))
                .dropEnd(LocalDate.of(2025, 3, 1))
                .build();
        addDropPeriodRepository.save(addDrop);

        registrationRepository.save(Registration.builder()
                .student(student).subjectOffering(mathOff).status(Registration.RegistrationStatus.CONFIRMED).createdAt(Instant.now()).build());
        registrationRepository.save(Registration.builder()
                .student(student).subjectOffering(progOff).status(Registration.RegistrationStatus.CONFIRMED).createdAt(Instant.now()).build());

        newsRepository.save(News.builder()
                .title("Spring Semester Registration Opens").content("Registration for Spring 2025 is now open.").category("Academic")
                .createdAt(Instant.now()).build());
        newsRepository.save(News.builder()
                .title("New Research Lab").content("KBTU opens Digital Innovation Lab.").category("Campus")
                .createdAt(Instant.now()).build());

        studentRequestRepository.save(StudentRequest.builder()
                .student(student).category("Reference").description("Need enrollment certificate").status(StudentRequest.RequestStatus.APPROVED)
                .createdAt(Instant.now()).build());

        Survey survey = Survey.builder().title("Course Quality Survey").startDate(LocalDate.now().minusDays(1)).endDate(LocalDate.now().plusDays(14))
                .semester(spring2025).build();
        surveyRepository.save(survey);

        gradeRepository.save(Grade.builder().student(student).subjectOffering(mathOff).type(Grade.GradeType.QUIZ).value(8).maxValue(10).createdAt(Instant.now()).build());
        gradeRepository.save(Grade.builder().student(student).subjectOffering(mathOff).type(Grade.GradeType.MIDTERM).value(75).maxValue(100).createdAt(Instant.now()).build());

        attendanceRepository.save(Attendance.builder().student(student).subjectOffering(mathOff).date(LocalDate.now().minusDays(1))
                .status(Attendance.AttendanceStatus.PRESENT).build());

        Charge charge = Charge.builder().student(student).amount(new BigDecimal("150000")).description("Tuition Spring 2025")
                .dueDate(LocalDate.of(2025, 2, 1)).status(Charge.ChargeStatus.PARTIAL).build();
        chargeRepository.save(charge);
        paymentRepository.save(Payment.builder().student(student).amount(new BigDecimal("75000")).date(LocalDate.now().minusDays(10)).charge(charge).build());

        checklistItemRepository.save(ChecklistItem.builder().student(student).title("Complete course registration").deadline(LocalDate.of(2025, 2, 15)).completed(false).linkToSection("/portal/course-registration").build());
        checklistItemRepository.save(ChecklistItem.builder().student(student).title("Complete course quality survey").deadline(LocalDate.now().plusDays(7)).completed(false).linkToSection("/portal/surveys").build());
    }
}
