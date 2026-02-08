package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.*;
import ru.kors.finalproject.repository.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddDropService {
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final RegistrationRepository registrationRepository;
    private final AddDropPeriodRepository addDropPeriodRepository;
    private final SubjectPrerequisiteRepository prerequisiteRepository;
    private final FinancialService financialService;

    public AddDropResult addCourse(Student student, Long subjectOfferingId) {
        var offering = subjectOfferingRepository.findById(subjectOfferingId);
        if (offering.isEmpty()) return AddDropResult.error("Subject not found");

        SubjectOffering so = offering.get();
        List<String> errors = new ArrayList<>();

        if (student.getCurrentSemester() == null || !student.getCurrentSemester().getId().equals(so.getSemester().getId()))
            errors.add("Subject is not for current semester");

        if (financialService.hasRegistrationLock(student))
            errors.add("Registration locked due to financial debt");

        var period = addDropPeriodRepository.findBySemesterId(so.getSemester().getId());
        if (period.isEmpty()) errors.add("Add/drop period not configured");
        else {
            LocalDate today = LocalDate.now();
            if (today.isBefore(period.get().getAddStart())) errors.add("Add period has not started");
            if (today.isAfter(period.get().getAddEnd())) errors.add("Add period has ended");
        }

        if (registrationRepository.existsByStudentIdAndSubjectOfferingId(student.getId(), subjectOfferingId))
            errors.add("Already registered for this subject");

        int registeredCredits = registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                .mapToInt(r -> r.getSubjectOffering().getSubject().getCredits())
                .sum();
        if (registeredCredits + so.getSubject().getCredits() > student.getProgram().getCreditLimit())
            errors.add("Credit limit exceeded");

        List<SubjectPrerequisite> prereqs = prerequisiteRepository.findBySubjectId(so.getSubject().getId());
        for (var p : prereqs) {
            boolean completed = registrationRepository.findByStudentIdWithDetails(student.getId()).stream()
                    .anyMatch(r -> r.getSubjectOffering().getSubject().getId().equals(p.getPrerequisite().getId()));
            if (!completed) errors.add("Prerequisite not completed: " + p.getPrerequisite().getName());
        }

        long regCount = registrationRepository.findByStudentId(student.getId()).stream()
                .filter(r -> r.getSubjectOffering().getId().equals(subjectOfferingId))
                .count();
        if (regCount > 0) errors.add("Already registered");

        long occupied = registrationRepository.countBySubjectOfferingIdAndStatusIn(subjectOfferingId,
                List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED));
        if (occupied >= so.getCapacity()) errors.add("No places available");

        if (!errors.isEmpty()) return AddDropResult.errors(errors);

        Registration reg = Registration.builder()
                .student(student).subjectOffering(so)
                .status(Registration.RegistrationStatus.CONFIRMED)
                .createdAt(java.time.Instant.now())
                .build();
        registrationRepository.save(reg);
        return AddDropResult.success("Subject added successfully");
    }

    public AddDropResult dropCourse(Student student, Long subjectOfferingId) {
        var reg = registrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), subjectOfferingId);
        if (reg.isEmpty()) return AddDropResult.error("Not registered for this subject");

        SubjectOffering so = reg.get().getSubjectOffering();
        var period = addDropPeriodRepository.findBySemesterId(so.getSemester().getId());
        if (period.isPresent()) {
            LocalDate today = LocalDate.now();
            if (today.isAfter(period.get().getDropEnd()))
                return AddDropResult.error("Drop period has ended");
        }
        registrationRepository.delete(reg.get());
        return AddDropResult.success("Subject dropped successfully");
    }

    public List<Registration> registrationsForStudent(Student student) {
        return registrationRepository.findByStudentIdWithDetails(student.getId());
    }

    public List<SubjectOffering> getAvailableForAdd(Student student) {
        if (student.getCurrentSemester() == null) return List.of();
        var offerings = subjectOfferingRepository.findBySemesterIdWithDetails(student.getCurrentSemester().getId());
        var registered = registrationRepository.findByStudentId(student.getId()).stream()
                .map(r -> r.getSubjectOffering().getId()).collect(Collectors.toSet());
        return offerings.stream()
                .filter(so -> !registered.contains(so.getId()))
                .filter(so -> registrationRepository.countBySubjectOfferingIdAndStatusIn(so.getId(),
                        List.of(Registration.RegistrationStatus.CONFIRMED, Registration.RegistrationStatus.SUBMITTED)) < so.getCapacity())
                .filter(so -> student.getProgram() != null && so.getSubject().getProgram() != null &&
                        student.getProgram().getId().equals(so.getSubject().getProgram().getId()))
                .toList();
    }

    public record AddDropResult(boolean success, String message, List<String> errors) {
        static AddDropResult success(String msg) { return new AddDropResult(true, msg, List.of()); }
        static AddDropResult error(String msg) { return new AddDropResult(false, msg, List.of(msg)); }
        static AddDropResult errors(List<String> errs) { return new AddDropResult(false, errs.get(0), errs); }
    }
}
