package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddDropService {
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final RegistrationWindowRepository registrationWindowRepository;
    private final SubjectPrerequisiteRepository prerequisiteRepository;
    private final MeetingTimeRepository meetingTimeRepository;
    private final HoldRepository holdRepository;
    private final FinalGradeRepository finalGradeRepository;
    private final FinancialService financialService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    @Transactional
    public AddDropResult addCourse(Student student, Long subjectOfferingId) {
        return enroll(student, subjectOfferingId, RegistrationWindow.WindowType.ADD_DROP, Registration.RegistrationStatus.CONFIRMED, false);
    }

    @Transactional
    public AddDropResult registerCourse(Student student, Long subjectOfferingId) {
        return enroll(student, subjectOfferingId, RegistrationWindow.WindowType.REGISTRATION, Registration.RegistrationStatus.SUBMITTED, false);
    }

    @Transactional
    public AddDropResult adminOverrideEnroll(Student student, Long subjectOfferingId) {
        return enroll(student, subjectOfferingId, RegistrationWindow.WindowType.REGISTRATION, Registration.RegistrationStatus.CONFIRMED, true);
    }

    @Transactional
    public AddDropResult dropCourse(Student student, Long subjectOfferingId) {
        return dropCourse(student, subjectOfferingId, null);
    }

    @Transactional
    public AddDropResult dropCourse(Student student, Long subjectOfferingId, String reason) {
        var reg = registrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), subjectOfferingId);
        if (reg.isEmpty() || reg.get().getStatus() == Registration.RegistrationStatus.DROPPED) {
            return AddDropResult.error("Not registered for this subject");
        }

        SubjectOffering so = reg.get().getSubjectOffering();
        var window = getActiveWindowFor(so, RegistrationWindow.WindowType.ADD_DROP);
        if (window.isEmpty()) {
            return AddDropResult.error("Add/drop window is closed");
        }

        Registration existing = reg.get();
        existing.setStatus(Registration.RegistrationStatus.DROPPED);
        existing.setDroppedAt(java.time.Instant.now());
        existing.setDropReason(reason);
        registrationRepository.save(existing);

        notificationService.notifyStudent(
                student.getEmail(),
                Notification.NotificationType.ENROLLMENT,
                "Course dropped",
                "You have dropped " + so.getSubject().getCode() + " " + so.getSubject().getName(),
                "/app/student/enrollments"
        );
        auditService.logStudentAction(
                student,
                "ENROLLMENT_DROPPED",
                "Registration",
                existing.getId(),
                "subjectOfferingId=" + subjectOfferingId + (reason != null ? ", reason=" + reason : "")
        );
        return AddDropResult.success("Subject dropped successfully");
    }

    public List<Registration> registrationsForStudent(Student student) {
        return registrationRepository.findActiveByStudentIdWithDetails(student.getId());
    }

    public List<Registration> rosterForSection(Long subjectOfferingId) {
        return registrationRepository.findBySubjectOfferingIdAndStatusIn(
                subjectOfferingId,
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)
        );
    }

    public List<SubjectOffering> getAvailableForAdd(Student student) {
        if (student.getCurrentSemester() == null) return List.of();
        var offerings = subjectOfferingRepository.findBySemesterIdWithDetails(student.getCurrentSemester().getId());
        var registered = registrationRepository.findActiveByStudentIdWithDetails(student.getId()).stream()
                .map(r -> r.getSubjectOffering().getId()).collect(Collectors.toSet());
        return offerings.stream()
                .filter(so -> !registered.contains(so.getId()))
                .filter(so -> registrationRepository.countBySubjectOfferingIdAndStatusIn(so.getId(),
                        List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)) < so.getCapacity())
                .filter(so -> student.getProgram() != null && so.getSubject().getProgram() != null &&
                        student.getProgram().getId().equals(so.getSubject().getProgram().getId()))
                .sorted(Comparator.comparing(so -> so.getSubject().getCode()))
                .toList();
    }

    public boolean hasActiveRegistrationHold(Student student) {
        return financialService.hasRegistrationLock(student) || holdRepository.findByStudentIdAndActiveTrue(student.getId()).stream()
                .anyMatch(h -> h.getType() == Hold.HoldType.ACADEMIC || h.getType() == Hold.HoldType.MANUAL);
    }

    private AddDropResult enroll(
            Student student,
            Long subjectOfferingId,
            RegistrationWindow.WindowType requiredWindowType,
            Registration.RegistrationStatus targetStatus,
            boolean adminOverride) {
        // Pessimistic lock on the offering row prevents two concurrent enrolments
        // from both seeing available capacity and over-filling the section.
        var offering = subjectOfferingRepository.findByIdForUpdate(subjectOfferingId);
        if (offering.isEmpty()) return AddDropResult.error("Subject not found");

        SubjectOffering so = offering.get();
        List<String> errors = new ArrayList<>();

        if (student.getCurrentSemester() == null || !student.getCurrentSemester().getId().equals(so.getSemester().getId())) {
            errors.add("Subject is not for current semester");
        }

        if (!adminOverride && hasActiveRegistrationHold(student)) {
            errors.add("Registration locked due to financial debt");
        }

        if (!adminOverride) {
            var window = getActiveWindowFor(so, requiredWindowType);
            if (window.isEmpty()) {
                errors.add(requiredWindowType + " window is not active");
            }
        }

        var existingReg = registrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), subjectOfferingId);
        if (existingReg.isPresent() && existingReg.get().getStatus() != Registration.RegistrationStatus.DROPPED) {
            errors.add("Already registered for this subject");
        }

        int registeredCredits = registrationRepository.findActiveByStudentIdWithDetails(student.getId()).stream()
                .mapToInt(r -> r.getSubjectOffering().getSubject().getCredits())
                .sum();

        if (student.getProgram() != null && registeredCredits + so.getSubject().getCredits() > student.getProgram().getCreditLimit()) {
            errors.add("Credit limit exceeded");
        }

        List<SubjectPrerequisite> prereqs = prerequisiteRepository.findBySubjectId(so.getSubject().getId());
        for (var p : prereqs) {
            boolean completed = finalGradeRepository.findByStudentId(student.getId()).stream()
                    .anyMatch(g -> g.isPublished()
                            && g.getNumericValue() >= 50
                            && g.getSubjectOffering().getSubject().getId().equals(p.getPrerequisite().getId()));
            if (!completed) {
                errors.add("Prerequisite not completed: " + p.getPrerequisite().getName());
            }
        }

        if (hasScheduleConflict(student, so)) {
            errors.add("Schedule conflict detected");
        }

        long occupied = registrationRepository.countBySubjectOfferingIdAndStatusIn(subjectOfferingId,
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED));
        if (!adminOverride && occupied >= so.getCapacity()) {
            errors.add("No places available");
        }

        if (!errors.isEmpty()) return AddDropResult.errors(errors);

        Registration reg = existingReg.orElseGet(() -> Registration.builder()
                .student(student)
                .subjectOffering(so)
                .createdAt(java.time.Instant.now())
                .build());
        reg.setStatus(targetStatus);
        reg.setDroppedAt(null);
        reg.setDropReason(null);
        registrationRepository.save(reg);

        notificationService.notifyStudent(
                student.getEmail(),
                Notification.NotificationType.ENROLLMENT,
                "Enrollment updated",
                "Enrollment for " + so.getSubject().getCode() + " is now " + targetStatus,
                "/app/student/enrollments"
        );
        auditService.logStudentAction(
                student,
                "ENROLLMENT_" + targetStatus,
                "Registration",
                reg.getId(),
                "subjectOfferingId=" + subjectOfferingId + ", override=" + adminOverride
        );
        return AddDropResult.success(targetStatus == Registration.RegistrationStatus.SUBMITTED
                ? "Course added to registration"
                : "Subject added successfully");
    }

    private java.util.Optional<RegistrationWindow> getActiveWindowFor(SubjectOffering so, RegistrationWindow.WindowType type) {
        LocalDate today = LocalDate.now();
        return registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(so.getSemester().getId(), type)
                .filter(w -> !today.isBefore(w.getStartDate()) && !today.isAfter(w.getEndDate()));
    }

    private boolean hasScheduleConflict(Student student, SubjectOffering targetOffering) {
        List<MeetingTime> targetTimes = resolveMeetingTimes(targetOffering);
        if (targetTimes.isEmpty()) {
            return false;
        }

        var activeRegs = registrationRepository.findActiveByStudentIdWithDetails(student.getId());
        for (Registration reg : activeRegs) {
            if (reg.getSubjectOffering().getId().equals(targetOffering.getId())) {
                continue;
            }
            List<MeetingTime> existingTimes = resolveMeetingTimes(reg.getSubjectOffering());
            for (MeetingTime existing : existingTimes) {
                for (MeetingTime candidate : targetTimes) {
                    if (existing.getDayOfWeek() == candidate.getDayOfWeek()
                            && existing.getStartTime().isBefore(candidate.getEndTime())
                            && candidate.getStartTime().isBefore(existing.getEndTime())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<MeetingTime> resolveMeetingTimes(SubjectOffering offering) {
        List<MeetingTime> times = meetingTimeRepository.findBySubjectOfferingId(offering.getId());
        if (!times.isEmpty()) {
            return times;
        }
        if (offering.getDayOfWeek() == null || offering.getStartTime() == null || offering.getEndTime() == null) {
            return List.of();
        }
        MeetingTime fallback = MeetingTime.builder()
                .subjectOffering(offering)
                .dayOfWeek(offering.getDayOfWeek())
                .startTime(offering.getStartTime())
                .endTime(offering.getEndTime())
                .room(offering.getRoom())
                .lessonType(offering.getLessonType())
                .build();
        return List.of(fallback);
    }

    public record AddDropResult(boolean success, String message, List<String> errors) {
        static AddDropResult success(String msg) { return new AddDropResult(true, msg, List.of()); }
        static AddDropResult error(String msg) { return new AddDropResult(false, msg, List.of(msg)); }
        static AddDropResult errors(List<String> errs) { return new AddDropResult(false, errs.get(0), errs); }
    }
}
