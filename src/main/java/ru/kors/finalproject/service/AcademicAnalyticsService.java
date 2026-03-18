package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicAnalyticsService {
    private final RegistrationRepository registrationRepository;
    private final GradeRepository gradeRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final AttendanceRepository attendanceRepository;
    private final HoldRepository holdRepository;
    private final ChargeRepository chargeRepository;
    private final StudentRequestRepository studentRequestRepository;
    private final FxRegistrationRepository fxRegistrationRepository;
    private final MobilityApplicationRepository mobilityApplicationRepository;
    private final ClearanceSheetRepository clearanceSheetRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final GradeChangeRequestRepository gradeChangeRequestRepository;
    private final TeacherStudentNoteRepository teacherStudentNoteRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final RegistrationWindowRepository registrationWindowRepository;

    @Transactional(readOnly = true)
    public StudentRiskDashboard buildStudentRiskDashboard(Student student) {
        StudentContext context = buildStudentContext(student);
        String facultyName = student.getFaculty() != null ? student.getFaculty().getName() : null;
        List<StudentCourseRisk> courseRisks = context.courseMetrics.values().stream()
                .map(this::toStudentCourseRisk)
                .sorted(Comparator.comparing(StudentCourseRisk::riskScore).reversed()
                        .thenComparing(StudentCourseRisk::courseCode))
                .toList();

        double overallScore = courseRisks.isEmpty()
                ? 0.0
                : courseRisks.stream().mapToDouble(StudentCourseRisk::riskScore).average().orElse(0.0);
        List<String> overallReasons = new ArrayList<>();
        courseRisks.stream()
                .filter(item -> item.level() != RiskLevel.STABLE)
                .limit(3)
                .forEach(item -> overallReasons.add(item.courseCode() + ": " + String.join("; ", item.reasons())));

        if (context.hasFinancialHold) {
            overallScore += 20.0;
            overallReasons.add("Active financial hold blocks registration-related actions");
        }
        if (context.activeHolds > 0 && !context.hasFinancialHold) {
            overallScore += 10.0;
            overallReasons.add("There are active administrative holds");
        }
        if (context.overdueCharges > 0) {
            overallScore += 10.0;
            overallReasons.add("There are overdue invoices on the account");
        }

        overallScore = Math.min(100.0, overallScore);

        return new StudentRiskDashboard(
                student.getId(),
                student.getName(),
                facultyName,
                context.activeSemesterName,
                riskLevel(overallScore),
                roundOne(overallScore),
                roundOne(context.publishedGpa),
                roundOne(context.overallAttendanceRate),
                context.hasFinancialHold,
                context.activeHolds,
                context.overdueCharges,
                context.openRequests,
                overallReasons,
                courseRisks
        );
    }

    @Transactional(readOnly = true)
    public StudentPlannerDashboard buildStudentPlannerDashboard(Student student) {
        StudentContext context = buildStudentContext(student);
        List<StudentPlannerCourse> courses = context.courseMetrics.values().stream()
                .map(this::toPlannerCourse)
                .sorted(Comparator.comparing(StudentPlannerCourse::courseCode))
                .toList();

        double maxProjectionGpa = courses.isEmpty()
                ? context.publishedGpa
                : calculateProjectedOverallGpa(context, courses.stream()
                .collect(Collectors.toMap(StudentPlannerCourse::sectionId, ignored -> 40.0)));

        return new StudentPlannerDashboard(
                student.getId(),
                student.getName(),
                context.activeSemesterId,
                context.activeSemesterName,
                roundOne(context.publishedGpa),
                context.publishedFinalCount,
                roundOne(maxProjectionGpa),
                courses
        );
    }

    @Transactional(readOnly = true)
    public StudentPlannerSimulation simulateStudentPlanner(Student student, Map<Long, Double> projectedFinalScores) {
        StudentContext context = buildStudentContext(student);
        Map<Long, Double> sanitized = projectedFinalScores == null
                ? Map.of()
                : projectedFinalScores.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> clamp(entry.getValue(), 0.0, 40.0)));

        List<StudentPlannerSimulationCourse> courses = context.courseMetrics.values().stream()
                .sorted(Comparator.comparing(metrics -> metrics.courseCode))
                .map(metrics -> {
                    double projectedFinal = sanitized.getOrDefault(metrics.sectionId, metrics.publishedFinalExam != null
                            ? metrics.publishedFinalExam
                            : 0.0);
                    double projectedTotal = clamp(metrics.attestation1 + metrics.attestation2 + projectedFinal, 0.0, 100.0);
                    GradeScale scale = gradeScale(projectedTotal);
                    return new StudentPlannerSimulationCourse(
                            metrics.sectionId,
                            metrics.courseCode,
                            metrics.courseName,
                            metrics.teacherName,
                            metrics.credits,
                            roundOne(metrics.attestation1),
                            roundOne(metrics.attestation2),
                            metrics.publishedFinalExam != null ? roundOne(metrics.publishedFinalExam) : null,
                            roundOne(projectedFinal),
                            roundOne(projectedTotal),
                            scale.letter(),
                            scale.points()
                    );
                })
                .toList();

        double projectedTermGpa = courses.isEmpty()
                ? 0.0
                : courses.stream().mapToDouble(StudentPlannerSimulationCourse::projectedPoints).average().orElse(0.0);
        double projectedOverallGpa = calculateProjectedOverallGpa(context, sanitized);

        return new StudentPlannerSimulation(
                roundOne(context.publishedGpa),
                roundOne(projectedTermGpa),
                roundOne(projectedOverallGpa),
                courses
        );
    }

    @Transactional(readOnly = true)
    public TeacherRiskDashboard buildTeacherRiskDashboard(Teacher teacher) {
        List<SubjectOffering> sections = selectFocusedTeacherSections(subjectOfferingRepository.findByTeacherIdWithDetails(teacher.getId()));
        List<TeacherSectionRisk> sectionRisks = new ArrayList<>();
        List<TeacherStudentRisk> allStudentRisks = new ArrayList<>();
        long pendingGradeChanges = gradeChangeRequestRepository.findByTeacherIdWithDetailsOrderByCreatedAtDesc(teacher.getId()).stream()
                .filter(request -> request.getStatus() == GradeChangeRequest.RequestStatus.SUBMITTED)
                .count();

        for (SubjectOffering section : sections) {
            SectionRiskComputation computation = computeSectionRisk(section);
            sectionRisks.add(new TeacherSectionRisk(
                    section.getId(),
                    section.getSubject() != null ? section.getSubject().getCode() : "-",
                    section.getSubject() != null ? section.getSubject().getName() : "-",
                    section.getSemester() != null ? section.getSemester().getName() : "-",
                    section.getCapacity(),
                    computation.enrolledCount,
                    roundOne(computation.attendanceRate),
                    computation.atRiskStudents.size(),
                    computation.pendingGradeChanges,
                    computation.unpublishedFinals,
                    riskLevel(computation.sectionRiskScore),
                    roundOne(computation.sectionRiskScore),
                    computation.sectionReasons,
                    section.getMeetingTimes().stream()
                            .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                            .map(mt -> new MeetingSlotDto(
                                    mt.getDayOfWeek().name(),
                                    mt.getStartTime() != null ? mt.getStartTime().toString() : null,
                                    mt.getEndTime() != null ? mt.getEndTime().toString() : null,
                                    mt.getRoom(),
                                    mt.getLessonType() != null ? mt.getLessonType().name() : null
                            ))
                            .toList()
            ));
            allStudentRisks.addAll(computation.atRiskStudents);
        }

        List<TeacherStudentRisk> topStudents = allStudentRisks.stream()
                .sorted(Comparator.comparing(TeacherStudentRisk::riskScore).reversed()
                        .thenComparing(TeacherStudentRisk::studentName))
                .limit(12)
                .toList();

        long sectionsNeedingAttention = sectionRisks.stream().filter(section -> section.level() != RiskLevel.STABLE).count();
        long unpublishedFinals = sectionRisks.stream().mapToLong(TeacherSectionRisk::unpublishedFinals).sum();

        return new TeacherRiskDashboard(
                teacher.getId(),
                teacher.getName(),
                sections.size(),
                sections.stream().filter(section -> section.getSemester() != null && section.getSemester().isCurrent()).count(),
                (int) topStudents.stream().map(TeacherStudentRisk::studentId).distinct().count(),
                (int) sectionsNeedingAttention,
                pendingGradeChanges,
                unpublishedFinals,
                sectionRisks,
                topStudents
        );
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsDashboard buildAdminAnalyticsDashboard() {
        List<StudentRiskDashboard> studentRiskDashboards = studentRepository.findAllWithDetails().stream()
                .map(this::buildStudentRiskDashboard)
                .toList();

        List<SubjectOffering> allSections = subjectOfferingRepository.findAllWithDetails();
        Semester currentSemester = allSections.stream()
                .map(SubjectOffering::getSemester)
                .filter(Objects::nonNull)
                .filter(Semester::isCurrent)
                .findFirst()
                .orElse(null);
        List<SubjectOffering> currentSections = currentSemester == null
                ? allSections
                : allSections.stream()
                .filter(section -> section.getSemester() != null && Objects.equals(section.getSemester().getId(), currentSemester.getId()))
                .toList();

        List<AdminOverloadedSection> overloadedSections = currentSections.stream()
                .map(section -> {
                    long occupied = registrationRepository.countBySubjectOfferingIdAndStatusIn(
                            section.getId(),
                            List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
                    );
                    double utilization = section.getCapacity() <= 0 ? 0.0 : occupied * 100.0 / section.getCapacity();
                    return new AdminOverloadedSection(
                            section.getId(),
                            section.getSubject() != null ? section.getSubject().getCode() : "-",
                            section.getSubject() != null ? section.getSubject().getName() : "-",
                            section.getSemester() != null ? section.getSemester().getName() : "-",
                            section.getTeacher() != null ? section.getTeacher().getName() : "Unassigned",
                            section.getSubject() != null && section.getSubject().getProgram() != null && section.getSubject().getProgram().getFaculty() != null
                                    ? section.getSubject().getProgram().getFaculty().getName()
                                    : "-",
                            section.getCapacity(),
                            occupied,
                            roundOne(utilization)
                    );
                })
                .filter(item -> item.utilizationPercent() >= 85.0)
                .sorted(Comparator.comparing(AdminOverloadedSection::utilizationPercent).reversed()
                        .thenComparing(AdminOverloadedSection::courseCode))
                .limit(12)
                .toList();

        Map<String, List<StudentRiskDashboard>> riskByFaculty = studentRiskDashboards.stream()
                .collect(Collectors.groupingBy(item -> item.facultyName() != null ? item.facultyName() : "Unassigned", LinkedHashMap::new, Collectors.toList()));
        List<AdminFacultyRisk> facultyRisks = riskByFaculty.entrySet().stream()
                .map(entry -> {
                    List<StudentRiskDashboard> items = entry.getValue();
                    long atRisk = items.stream().filter(item -> item.level() == RiskLevel.AT_RISK).count();
                    long medium = items.stream().filter(item -> item.level() == RiskLevel.MEDIUM).count();
                    double avgRisk = items.stream().mapToDouble(StudentRiskDashboard::riskScore).average().orElse(0.0);
                    double avgAttendance = items.stream().mapToDouble(StudentRiskDashboard::attendanceRate).average().orElse(0.0);
                    long holds = items.stream().filter(StudentRiskDashboard::hasFinancialHold).count();
                    return new AdminFacultyRisk(
                            entry.getKey(),
                            items.size(),
                            (int) atRisk,
                            (int) medium,
                            roundOne(avgRisk),
                            roundOne(avgAttendance),
                            (int) holds
                    );
                })
                .sorted(Comparator.comparing(AdminFacultyRisk::averageRisk).reversed())
                .toList();

        List<StudentRequest> requests = studentRequestRepository.findAllWithDetails();
        Map<String, Long> requestCategories = requests.stream()
                .collect(Collectors.groupingBy(request -> request.getCategory() != null ? request.getCategory() : "General", LinkedHashMap::new, Collectors.counting()));
        List<AdminRequestLoad> requestLoads = requestCategories.entrySet().stream()
                .map(entry -> new AdminRequestLoad(entry.getKey(), entry.getValue().intValue()))
                .sorted(Comparator.comparing(AdminRequestLoad::count).reversed())
                .toList();

        List<AdminWorkflowSummary> workflowSummary = List.of(
                new AdminWorkflowSummary("Requests", requests.stream().filter(request -> request.getStatus() != StudentRequest.RequestStatus.DONE).count()),
                new AdminWorkflowSummary("FX", fxRegistrationRepository.findAllWithDetailsOrderByCreatedAtDesc().stream().filter(fx -> fx.getStatus() != FxRegistration.FxStatus.CONFIRMED).count()),
                new AdminWorkflowSummary("Mobility", mobilityApplicationRepository.findAllWithStudent().stream()
                        .filter(app -> app.getStatus() != MobilityApplication.MobilityStatus.APPROVED
                                && app.getStatus() != MobilityApplication.MobilityStatus.REJECTED)
                        .count()),
                new AdminWorkflowSummary("Clearance", clearanceSheetRepository.findAllWithStudentAndCheckpoints().stream()
                        .filter(sheet -> sheet.getStatus() != ClearanceSheet.ClearanceStatus.CLEARED)
                        .count()),
                new AdminWorkflowSummary("Grade Changes", gradeChangeRequestRepository.findByStatusWithDetailsOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus.SUBMITTED).size())
        );

        List<StudentRiskSnapshot> criticalStudents = studentRiskDashboards.stream()
                .filter(item -> item.level() != RiskLevel.STABLE)
                .sorted(Comparator.comparing(StudentRiskDashboard::riskScore).reversed())
                .limit(10)
                .map(item -> new StudentRiskSnapshot(
                        item.studentId(),
                        item.studentName(),
                        item.facultyName(),
                        item.level(),
                        item.riskScore(),
                        item.reasons().stream().findFirst().orElse("Needs review")
                ))
                .toList();

        return new AdminAnalyticsDashboard(
                new AdminAnalyticsMetrics(
                        studentRiskDashboards.size(),
                        teacherRepository.count(),
                        currentSections.size(),
                        requestLoads.stream().mapToInt(AdminRequestLoad::count).sum(),
                        holdRepository.findActiveWithStudent().size(),
                        registrationWindowRepository.findAllWithSemesterOrderByStartDateDesc().stream()
                                .filter(window -> window.isActive()
                                        && !LocalDate.now().isBefore(window.getStartDate())
                                        && !LocalDate.now().isAfter(window.getEndDate()))
                                .count()
                ),
                facultyRisks,
                overloadedSections,
                requestLoads,
                workflowSummary,
                criticalStudents
        );
    }

    @Transactional(readOnly = true)
    protected SectionRiskComputation computeSectionRisk(SubjectOffering section) {
        List<Registration> roster = registrationRepository.findBySubjectOfferingIdAndStatusInWithDetails(
                section.getId(),
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
        );
        List<Attendance> attendance = attendanceRepository.findBySubjectOfferingIdOrderByDateDescWithDetails(section.getId());
        List<Grade> grades = gradeRepository.findBySubjectOfferingIdWithDetails(section.getId());
        List<FinalGrade> finalGrades = finalGradeRepository.findBySubjectOfferingIdWithDetails(section.getId());
        List<GradeChangeRequest> gradeChanges = gradeChangeRequestRepository.findBySubjectOfferingIdOrderByCreatedAtDesc(section.getId());
        Map<Long, TeacherStudentNote.RiskFlag> noteFlags = section.getTeacher() == null
                ? Map.of()
                : teacherStudentNoteRepository.findByTeacherIdAndSubjectOfferingIdWithDetailsOrderByCreatedAtDesc(section.getTeacher().getId(), section.getId())
                .stream()
                .collect(Collectors.toMap(
                        item -> item.getStudent().getId(),
                        TeacherStudentNote::getRiskFlag,
                        (left, right) -> left
                ));

        Map<Long, CourseMetrics> metricsByStudent = new LinkedHashMap<>();
        for (Registration registration : roster) {
            Student student = registration.getStudent();
            CourseMetrics metrics = new CourseMetrics(section, student);
            metricsByStudent.put(student.getId(), metrics);
        }

        grades.forEach(grade -> {
            CourseMetrics metrics = metricsByStudent.get(grade.getStudent().getId());
            if (metrics != null) {
                applyGrade(metrics, grade);
            }
        });
        finalGrades.forEach(finalGrade -> {
            CourseMetrics metrics = metricsByStudent.get(finalGrade.getStudent().getId());
            if (metrics != null) {
                applyFinalGrade(metrics, finalGrade);
            }
        });
        attendance.forEach(record -> {
            CourseMetrics metrics = metricsByStudent.get(record.getStudent().getId());
            if (metrics != null) {
                applyAttendance(metrics, record);
            }
        });

        List<TeacherStudentRisk> atRiskStudents = new ArrayList<>();
        for (CourseMetrics metrics : metricsByStudent.values()) {
            TeacherStudentNote.RiskFlag noteFlag = noteFlags.getOrDefault(metrics.studentId, TeacherStudentNote.RiskFlag.NONE);
            boolean lowAttendanceFlag = noteFlag == TeacherStudentNote.RiskFlag.LOW_ATTENDANCE || noteFlag == TeacherStudentNote.RiskFlag.COMBINED_RISK;
            boolean lowGradesFlag = noteFlag == TeacherStudentNote.RiskFlag.LOW_GRADES || noteFlag == TeacherStudentNote.RiskFlag.COMBINED_RISK;
            finalizeMetrics(metrics, lowAttendanceFlag, lowGradesFlag);
            if (metrics.riskLevel != RiskLevel.STABLE) {
                atRiskStudents.add(new TeacherStudentRisk(
                        metrics.studentId,
                        metrics.studentName,
                        metrics.studentEmail,
                        metrics.sectionId,
                        metrics.courseCode,
                        metrics.courseName,
                        metrics.riskLevel,
                        roundOne(metrics.riskScore),
                        metrics.riskReasons,
                        roundOne(metrics.attendanceRate),
                        roundOne(metrics.attestation1 + metrics.attestation2),
                        metrics.publishedTotal != null ? roundOne(metrics.publishedTotal) : null
                ));
            }
        }

        long present = attendance.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.PRESENT).count();
        long late = attendance.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.LATE).count();
        long absent = attendance.stream().filter(item -> item.getStatus() == Attendance.AttendanceStatus.ABSENT).count();
        long totalAttendance = present + late + absent;
        double attendanceRate = totalAttendance == 0 ? 0.0 : (present + late) * 100.0 / totalAttendance;
        long pendingGradeChanges = gradeChanges.stream().filter(item -> item.getStatus() == GradeChangeRequest.RequestStatus.SUBMITTED).count();
        long unpublishedFinals = Math.max(0, roster.size() - finalGrades.stream().filter(FinalGrade::isPublished).count());

        double sectionRiskScore = 0.0;
        List<String> reasons = new ArrayList<>();
        if (!roster.isEmpty()) {
            double riskyRatio = atRiskStudents.size() * 100.0 / roster.size();
            if (riskyRatio >= 50.0) {
                sectionRiskScore += 45.0;
                reasons.add("More than half of the roster is at risk");
            } else if (riskyRatio >= 25.0) {
                sectionRiskScore += 25.0;
                reasons.add("A large share of the roster needs attention");
            } else if (riskyRatio > 0.0) {
                sectionRiskScore += 12.0;
                reasons.add("There are students at risk in this section");
            }
        }
        if (attendanceRate < 75.0 && totalAttendance > 0) {
            sectionRiskScore += 25.0;
            reasons.add("Attendance trend is weak");
        } else if (attendanceRate < 85.0 && totalAttendance > 0) {
            sectionRiskScore += 12.0;
            reasons.add("Attendance is below the target band");
        }
        if (pendingGradeChanges > 0) {
            sectionRiskScore += Math.min(15.0, pendingGradeChanges * 3.0);
            reasons.add("There are pending grade change requests");
        }
        if (unpublishedFinals > 0) {
            sectionRiskScore += Math.min(15.0, unpublishedFinals * 2.0);
            reasons.add("Not all final grades are published");
        }

        return new SectionRiskComputation(
                roster.size(),
                attendanceRate,
                pendingGradeChanges,
                unpublishedFinals,
                Math.min(100.0, sectionRiskScore),
                reasons,
                atRiskStudents
        );
    }

    private StudentContext buildStudentContext(Student student) {
        List<Registration> registrations = registrationRepository.findByStudentIdWithDetails(student.getId());
        Semester activeSemester = resolveActiveSemester(student, registrations);
        Long activeSemesterId = activeSemester != null ? activeSemester.getId() : null;

        Map<Long, CourseMetrics> metricsByOffering = registrations.stream()
                .filter(registration -> registration.getStatus() != Registration.RegistrationStatus.DROPPED)
                .filter(registration -> activeSemesterId == null
                        || (registration.getSubjectOffering() != null
                        && registration.getSubjectOffering().getSemester() != null
                        && Objects.equals(registration.getSubjectOffering().getSemester().getId(), activeSemesterId)))
                .filter(registration -> registration.getSubjectOffering() != null && registration.getSubjectOffering().getSubject() != null)
                .collect(Collectors.toMap(
                        registration -> registration.getSubjectOffering().getId(),
                        registration -> new CourseMetrics(registration.getSubjectOffering(), student),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<Grade> grades = gradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        List<FinalGrade> finalGrades = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(student.getId());
        List<Attendance> attendance = attendanceRepository.findByStudentIdWithDetails(student.getId());

        grades.stream()
                .filter(grade -> grade.getSubjectOffering() != null && metricsByOffering.containsKey(grade.getSubjectOffering().getId()))
                .forEach(grade -> applyGrade(metricsByOffering.get(grade.getSubjectOffering().getId()), grade));
        finalGrades.stream()
                .filter(finalGrade -> finalGrade.getSubjectOffering() != null && metricsByOffering.containsKey(finalGrade.getSubjectOffering().getId()))
                .forEach(finalGrade -> applyFinalGrade(metricsByOffering.get(finalGrade.getSubjectOffering().getId()), finalGrade));
        attendance.stream()
                .filter(record -> record.getSubjectOffering() != null && metricsByOffering.containsKey(record.getSubjectOffering().getId()))
                .forEach(record -> applyAttendance(metricsByOffering.get(record.getSubjectOffering().getId()), record));

        metricsByOffering.values().forEach(metrics -> finalizeMetrics(metrics, false, false));

        List<Hold> holds = holdRepository.findByStudentIdAndActiveTrue(student.getId());
        List<Charge> charges = chargeRepository.findByStudentIdWithDetailsOrderByDueDateDesc(student.getId());
        long overdueCharges = charges.stream().filter(charge -> charge.getStatus() == Charge.ChargeStatus.OVERDUE).count();
        long openRequests = studentRequestRepository.findByStudentIdWithDetailsOrderByCreatedAtDesc(student.getId()).stream()
                .filter(request -> request.getStatus() != StudentRequest.RequestStatus.DONE
                        && request.getStatus() != StudentRequest.RequestStatus.REJECTED)
                .count();

        long totalPresent = metricsByOffering.values().stream().mapToLong(item -> item.present).sum();
        long totalLate = metricsByOffering.values().stream().mapToLong(item -> item.late).sum();
        long totalAbsent = metricsByOffering.values().stream().mapToLong(item -> item.absent).sum();
        long totalAttendance = totalPresent + totalLate + totalAbsent;
        double overallAttendanceRate = totalAttendance == 0 ? 0.0 : (totalPresent + totalLate) * 100.0 / totalAttendance;

        return new StudentContext(
                student.getId(),
                metricsByOffering,
                activeSemesterId,
                activeSemester != null ? activeSemester.getName() : "No semester",
                finalGrades.isEmpty() ? 0.0 : finalGrades.stream().mapToDouble(FinalGrade::getPoints).average().orElse(0.0),
                finalGrades.size(),
                overallAttendanceRate,
                holds.stream().anyMatch(hold -> hold.getType() == Hold.HoldType.FINANCIAL),
                holds.size(),
                overdueCharges,
                openRequests
        );
    }

    private StudentCourseRisk toStudentCourseRisk(CourseMetrics metrics) {
        return new StudentCourseRisk(
                metrics.sectionId,
                metrics.courseCode,
                metrics.courseName,
                metrics.teacherName,
                metrics.credits,
                roundOne(metrics.attestation1),
                roundOne(metrics.attestation2),
                metrics.publishedFinalExam != null ? roundOne(metrics.publishedFinalExam) : null,
                metrics.publishedTotal != null ? roundOne(metrics.publishedTotal) : null,
                roundOne(metrics.attendanceRate),
                metrics.present,
                metrics.late,
                metrics.absent,
                metrics.riskLevel,
                roundOne(metrics.riskScore),
                metrics.riskReasons,
                metrics.neededForPass,
                metrics.neededForB,
                metrics.neededForA
        );
    }

    private StudentPlannerCourse toPlannerCourse(CourseMetrics metrics) {
        double currentSubtotal = metrics.attestation1 + metrics.attestation2;
        return new StudentPlannerCourse(
                metrics.sectionId,
                metrics.courseCode,
                metrics.courseName,
                metrics.teacherName,
                metrics.credits,
                roundOne(metrics.attestation1),
                roundOne(metrics.attestation2),
                metrics.publishedFinalExam != null ? roundOne(metrics.publishedFinalExam) : null,
                metrics.publishedTotal != null ? roundOne(metrics.publishedTotal) : null,
                metrics.publishedLetter,
                roundOne(currentSubtotal),
                100.0,
                metrics.neededForPass,
                metrics.neededForB,
                metrics.neededForA
        );
    }

    private double calculateProjectedOverallGpa(StudentContext context, Map<Long, Double> projectedFinalScores) {
        List<FinalGrade> historicalFinals = finalGradeRepository.findByStudentIdAndPublishedTrueWithDetails(context.studentId);
        Map<Long, FinalGrade> publishedCurrentSemester = historicalFinals.stream()
                .filter(finalGrade -> finalGrade.getSubjectOffering() != null
                        && context.courseMetrics.containsKey(finalGrade.getSubjectOffering().getId()))
                .collect(Collectors.toMap(finalGrade -> finalGrade.getSubjectOffering().getId(), Function.identity(), (left, right) -> left));

        List<Double> points = historicalFinals.stream()
                .filter(finalGrade -> finalGrade.getSubjectOffering() == null
                        || !context.courseMetrics.containsKey(finalGrade.getSubjectOffering().getId()))
                .map(FinalGrade::getPoints)
                .collect(Collectors.toCollection(ArrayList::new));

        for (CourseMetrics metrics : context.courseMetrics.values()) {
            Double projectedFinal = projectedFinalScores.get(metrics.sectionId);
            if (projectedFinal == null && metrics.publishedTotal == null && !publishedCurrentSemester.containsKey(metrics.sectionId)) {
                continue;
            }
            if (projectedFinal == null && publishedCurrentSemester.containsKey(metrics.sectionId)) {
                points.add(publishedCurrentSemester.get(metrics.sectionId).getPoints());
                continue;
            }
            double projectedTotal = clamp(metrics.attestation1 + metrics.attestation2 + (projectedFinal != null ? projectedFinal : 0.0), 0.0, 100.0);
            points.add(gradeScale(projectedTotal).points());
        }

        return points.isEmpty() ? 0.0 : points.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private List<SubjectOffering> selectFocusedTeacherSections(List<SubjectOffering> sections) {
        List<SubjectOffering> current = sections.stream()
                .filter(section -> section.getSemester() != null && section.getSemester().isCurrent())
                .sorted(sectionComparator())
                .toList();
        if (!current.isEmpty()) {
            return current;
        }
        Semester latestSemester = sections.stream()
                .map(SubjectOffering::getSemester)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        if (latestSemester == null) {
            return List.of();
        }
        return sections.stream()
                .filter(section -> section.getSemester() != null && Objects.equals(section.getSemester().getId(), latestSemester.getId()))
                .sorted(sectionComparator())
                .toList();
    }

    private Comparator<SubjectOffering> sectionComparator() {
        return Comparator
                .comparing((SubjectOffering section) -> section.getSemester() != null ? section.getSemester().getStartDate() : null,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(section -> section.getSubject() != null ? section.getSubject().getCode() : "");
    }

    private Semester resolveActiveSemester(Student student, List<Registration> registrations) {
        if (student.getCurrentSemester() != null) {
            return student.getCurrentSemester();
        }
        return registrations.stream()
                .map(registration -> registration.getSubjectOffering() != null ? registration.getSubjectOffering().getSemester() : null)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Semester::getStartDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private void applyGrade(CourseMetrics metrics, Grade grade) {
        String componentName = normalizeComponent(grade);
        if (componentName.contains("attestation 1")) {
            metrics.attestation1 = grade.getGradeValue();
        } else if (componentName.contains("attestation 2")) {
            metrics.attestation2 = grade.getGradeValue();
        } else if (componentName.contains("final") || grade.getType() == Grade.GradeType.FINAL) {
            metrics.publishedFinalExam = grade.getGradeValue();
        }
    }

    private void applyFinalGrade(CourseMetrics metrics, FinalGrade finalGrade) {
        metrics.publishedTotal = finalGrade.getNumericValue();
        metrics.publishedLetter = finalGrade.getLetterValue();
        metrics.publishedPoints = finalGrade.getPoints();
    }

    private void applyAttendance(CourseMetrics metrics, Attendance attendance) {
        if (attendance.getStatus() == Attendance.AttendanceStatus.PRESENT) {
            metrics.present += 1;
        } else if (attendance.getStatus() == Attendance.AttendanceStatus.LATE) {
            metrics.late += 1;
        } else if (attendance.getStatus() == Attendance.AttendanceStatus.ABSENT) {
            metrics.absent += 1;
        }
    }

    private void finalizeMetrics(CourseMetrics metrics, boolean lowAttendanceFlag, boolean lowGradesFlag) {
        int totalAttendance = metrics.present + metrics.late + metrics.absent;
        metrics.attendanceRate = totalAttendance == 0 ? 0.0 : (metrics.present + metrics.late) * 100.0 / totalAttendance;

        double riskScore = 0.0;
        List<String> reasons = new ArrayList<>();
        if (totalAttendance > 0) {
            if (metrics.attendanceRate < 70.0) {
                riskScore += 40.0;
                reasons.add("attendance is below 70%");
            } else if (metrics.attendanceRate < 80.0) {
                riskScore += 25.0;
                reasons.add("attendance is below 80%");
            } else if (metrics.attendanceRate < 90.0) {
                riskScore += 10.0;
                reasons.add("attendance is below 90%");
            }
            if (metrics.absent >= 3) {
                riskScore += 10.0;
                reasons.add("multiple absences");
            }
        }

        double subtotal = metrics.attestation1 + metrics.attestation2;
        if (subtotal > 0 || metrics.publishedFinalExam != null || metrics.publishedTotal != null) {
            if (subtotal < 30.0) {
                riskScore += 30.0;
                reasons.add("attestation subtotal is weak");
            } else if (subtotal < 40.0) {
                riskScore += 20.0;
                reasons.add("attestation subtotal needs attention");
            } else if (subtotal < 50.0) {
                riskScore += 10.0;
                reasons.add("attestation subtotal is below target");
            }
        }

        if (metrics.publishedTotal != null) {
            if (metrics.publishedTotal < 50.0) {
                riskScore += 40.0;
                reasons.add("published final result is failing");
            } else if (metrics.publishedTotal < 65.0) {
                riskScore += 20.0;
                reasons.add("published final result is low");
            }
        }

        if (lowAttendanceFlag) {
            riskScore += 10.0;
            reasons.add("teacher flagged attendance concern");
        }
        if (lowGradesFlag) {
            riskScore += 10.0;
            reasons.add("teacher flagged grade concern");
        }

        metrics.riskScore = Math.min(100.0, riskScore);
        metrics.riskLevel = riskLevel(metrics.riskScore);
        metrics.riskReasons = reasons;
        metrics.neededForPass = neededFinal(metrics.attestation1 + metrics.attestation2, 50);
        metrics.neededForB = neededFinal(metrics.attestation1 + metrics.attestation2, 80);
        metrics.neededForA = neededFinal(metrics.attestation1 + metrics.attestation2, 95);
    }

    private String normalizeComponent(Grade grade) {
        String componentName = grade.getComponent() != null ? grade.getComponent().getName() : "";
        String comment = grade.getComment() != null ? grade.getComment() : "";
        return (componentName + " " + comment).toLowerCase(Locale.ROOT);
    }

    private RiskLevel riskLevel(double score) {
        if (score >= 70.0) {
            return RiskLevel.AT_RISK;
        }
        if (score >= 35.0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.STABLE;
    }

    private Double neededFinal(double subtotal, double targetTotal) {
        double needed = targetTotal - subtotal;
        if (needed <= 0) {
            return 0.0;
        }
        if (needed > 40.0) {
            return null;
        }
        return roundOne(needed);
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

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum RiskLevel {
        STABLE,
        MEDIUM,
        AT_RISK
    }

    public record StudentRiskDashboard(
            Long studentId,
            String studentName,
            String facultyName,
            String semesterName,
            RiskLevel level,
            double riskScore,
            double publishedGpa,
            double attendanceRate,
            boolean hasFinancialHold,
            long activeHolds,
            long overdueCharges,
            long openRequests,
            List<String> reasons,
            List<StudentCourseRisk> courses
    ) {
    }

    public record StudentCourseRisk(
            Long sectionId,
            String courseCode,
            String courseName,
            String teacherName,
            int credits,
            double attestation1,
            double attestation2,
            Double finalExam,
            Double totalScore,
            double attendanceRate,
            int present,
            int late,
            int absent,
            RiskLevel level,
            double riskScore,
            List<String> reasons,
            Double neededForPass,
            Double neededForB,
            Double neededForA
    ) {
    }

    public record StudentPlannerDashboard(
            Long studentId,
            String studentName,
            Long semesterId,
            String semesterName,
            double currentPublishedGpa,
            int publishedFinalCount,
            double maxProjectionGpa,
            List<StudentPlannerCourse> courses
    ) {
    }

    public record StudentPlannerCourse(
            Long sectionId,
            String courseCode,
            String courseName,
            String teacherName,
            int credits,
            double attestation1,
            double attestation2,
            Double publishedFinal,
            Double publishedTotal,
            String publishedLetter,
            double subtotal,
            double maxTotal,
            Double neededForPass,
            Double neededForB,
            Double neededForA
    ) {
    }

    public record StudentPlannerSimulation(
            double currentPublishedGpa,
            double projectedTermGpa,
            double projectedOverallGpa,
            List<StudentPlannerSimulationCourse> courses
    ) {
    }

    public record StudentPlannerSimulationCourse(
            Long sectionId,
            String courseCode,
            String courseName,
            String teacherName,
            int credits,
            double attestation1,
            double attestation2,
            Double publishedFinal,
            double projectedFinal,
            double projectedTotal,
            String projectedLetter,
            double projectedPoints
    ) {
    }

    public record TeacherRiskDashboard(
            Long teacherId,
            String teacherName,
            long totalSections,
            long currentSections,
            int atRiskStudents,
            int sectionsNeedingAttention,
            long pendingGradeChanges,
            long unpublishedFinals,
            List<TeacherSectionRisk> sections,
            List<TeacherStudentRisk> students
    ) {
    }

    public record TeacherSectionRisk(
            Long sectionId,
            String courseCode,
            String courseName,
            String semesterName,
            int capacity,
            int enrolledCount,
            double attendanceRate,
            long atRiskStudents,
            long pendingGradeChanges,
            long unpublishedFinals,
            RiskLevel level,
            double riskScore,
            List<String> reasons,
            List<MeetingSlotDto> meetingTimes
    ) {
    }

    public record TeacherStudentRisk(
            Long studentId,
            String studentName,
            String studentEmail,
            Long sectionId,
            String courseCode,
            String courseName,
            RiskLevel level,
            double riskScore,
            List<String> reasons,
            double attendanceRate,
            double attestationSubtotal,
            Double finalTotal
    ) {
    }

    public record MeetingSlotDto(String dayOfWeek, String startTime, String endTime, String room, String lessonType) {
    }

    public record AdminAnalyticsDashboard(
            AdminAnalyticsMetrics metrics,
            List<AdminFacultyRisk> facultyRisks,
            List<AdminOverloadedSection> overloadedSections,
            List<AdminRequestLoad> requestLoads,
            List<AdminWorkflowSummary> workflowSummary,
            List<StudentRiskSnapshot> criticalStudents
    ) {
    }

    public record AdminAnalyticsMetrics(
            long students,
            long teachers,
            long currentSections,
            long requests,
            long activeHolds,
            long openWindows
    ) {
    }

    public record AdminFacultyRisk(
            String facultyName,
            int studentCount,
            int atRiskStudents,
            int mediumRiskStudents,
            double averageRisk,
            double averageAttendance,
            int studentsWithFinancialHolds
    ) {
    }

    public record AdminOverloadedSection(
            Long sectionId,
            String courseCode,
            String courseName,
            String semesterName,
            String teacherName,
            String facultyName,
            int capacity,
            long enrolledCount,
            double utilizationPercent
    ) {
    }

    public record AdminRequestLoad(String category, int count) {
    }

    public record AdminWorkflowSummary(String workflowType, long count) {
    }

    public record StudentRiskSnapshot(
            Long studentId,
            String studentName,
            String facultyName,
            RiskLevel level,
            double riskScore,
            String primaryReason
    ) {
    }

    private record StudentContext(
            Long studentId,
            Map<Long, CourseMetrics> courseMetrics,
            Long activeSemesterId,
            String activeSemesterName,
            double publishedGpa,
            int publishedFinalCount,
            double overallAttendanceRate,
            boolean hasFinancialHold,
            long activeHolds,
            long overdueCharges,
            long openRequests
    ) {
    }

    private static final class CourseMetrics {
        private final Long studentId;
        private final String studentName;
        private final String studentEmail;
        private final Long sectionId;
        private final String courseCode;
        private final String courseName;
        private final String teacherName;
        private final int credits;
        private double attestation1;
        private double attestation2;
        private Double publishedFinalExam;
        private Double publishedTotal;
        private String publishedLetter;
        private Double publishedPoints;
        private int present;
        private int late;
        private int absent;
        private double attendanceRate;
        private double riskScore;
        private RiskLevel riskLevel = RiskLevel.STABLE;
        private List<String> riskReasons = List.of();
        private Double neededForPass;
        private Double neededForB;
        private Double neededForA;

        private CourseMetrics(SubjectOffering offering, Student student) {
            this.studentId = student != null ? student.getId() : null;
            this.studentName = student != null ? student.getName() : null;
            this.studentEmail = student != null ? student.getEmail() : null;
            this.sectionId = offering.getId();
            this.courseCode = offering.getSubject() != null ? offering.getSubject().getCode() : "-";
            this.courseName = offering.getSubject() != null ? offering.getSubject().getName() : "-";
            this.teacherName = offering.getTeacher() != null ? offering.getTeacher().getName() : null;
            this.credits = offering.getSubject() != null ? offering.getSubject().getCredits() : 0;
        }
    }

    protected record SectionRiskComputation(
            int enrolledCount,
            double attendanceRate,
            long pendingGradeChanges,
            long unpublishedFinals,
            double sectionRiskScore,
            List<String> sectionReasons,
            List<TeacherStudentRisk> atRiskStudents
    ) {
    }

    private record GradeScale(String letter, double points) {
    }
}
