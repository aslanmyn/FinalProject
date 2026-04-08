package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.MeetingTime;
import ru.kors.finalproject.entity.PlannedRegistration;
import ru.kors.finalproject.entity.ProgramCurriculumItem;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.repository.PlannedRegistrationRepository;
import ru.kors.finalproject.repository.ProgramCurriculumItemRepository;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NextSemesterPlanningService {
    private static final int MAX_SELECTIONS = 5;

    private final SemesterRepository semesterRepository;
    private final ProgramCurriculumItemRepository programCurriculumItemRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final PlannedRegistrationRepository plannedRegistrationRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public NextSemesterPlanOverview getOverview(Student student) {
        PlanningTarget target = resolvePlanningTarget(student).orElse(null);
        if (target == null) {
            return new NextSemesterPlanOverview(
                    null,
                    null,
                    null,
                    null,
                    false,
                    MAX_SELECTIONS,
                    0,
                    "Next-semester planning is not available for this student right now.",
                    List.of(),
                    List.of()
            );
        }

        List<ProgramCurriculumItem> curriculumItems = programCurriculumItemRepository
                .findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(
                        student.getProgram().getId(),
                        target.academicYear(),
                        target.semesterNumber()
                );
        if (curriculumItems.isEmpty()) {
            return new NextSemesterPlanOverview(
                    target.semester().getId(),
                    target.semester().getName(),
                    target.academicYear(),
                    target.semesterNumber(),
                    false,
                    MAX_SELECTIONS,
                    0,
                    "No curriculum items are configured yet for the next semester.",
                    List.of(),
                    List.of()
            );
        }

        List<SubjectOffering> offerings = subjectOfferingRepository.findBySemesterIdWithDetails(target.semester().getId()).stream()
                .filter(offering -> offering.getSubject() != null)
                .filter(offering -> curriculumItems.stream().anyMatch(item -> Objects.equals(item.getSubject().getId(), offering.getSubject().getId())))
                .sorted(Comparator.comparing((SubjectOffering offering) -> offering.getSubject().getCode(), Comparator.nullsLast(String::compareTo))
                        .thenComparing(SubjectOffering::getId))
                .toList();

        List<PlannedRegistration> selections = plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(student.getId(), target.semester().getId());
        Set<Long> selectedOfferingIds = selections.stream()
                .map(selection -> selection.getSubjectOffering().getId())
                .collect(Collectors.toSet());
        Map<Long, PlannedRegistration> selectedBySubjectId = selections.stream()
                .filter(selection -> selection.getSubjectOffering() != null && selection.getSubjectOffering().getSubject() != null)
                .collect(Collectors.toMap(
                        selection -> selection.getSubjectOffering().getSubject().getId(),
                        selection -> selection,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<NextSemesterSelectionDto> savedSelections = selections.stream()
                .map(this::toSelectionDto)
                .toList();

        List<NextSemesterSubjectOptionDto> subjectOptions = curriculumItems.stream()
                .map(item -> {
                    List<SubjectOffering> subjectOfferings = offerings.stream()
                            .filter(offering -> Objects.equals(offering.getSubject().getId(), item.getSubject().getId()))
                            .toList();
                    PlannedRegistration selected = selectedBySubjectId.get(item.getSubject().getId());
                    List<NextSemesterSectionOptionDto> sectionOptions = subjectOfferings.stream()
                            .map(offering -> {
                                List<String> reasons = evaluateSelection(student, target, offering, selections);
                                boolean selectedFlag = selectedOfferingIds.contains(offering.getId());
                                if (selectedFlag) {
                                    reasons = List.of();
                                }
                                return new NextSemesterSectionOptionDto(
                                        offering.getId(),
                                        offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                                        offering.getCapacity(),
                                        plannedRegistrationRepository.countBySubjectOfferingId(offering.getId()),
                                        buildMeetingTimes(offering),
                                        selectedFlag,
                                        reasons,
                                        reasons.isEmpty() || selectedFlag
                                );
                            })
                            .toList();
                    return new NextSemesterSubjectOptionDto(
                            item.getSubject().getId(),
                            item.getSubject().getCode(),
                            item.getSubject().getName(),
                            item.getSubject().getCredits(),
                            item.isRequired(),
                            item.getDisplayOrder(),
                            selected != null ? selected.getSubjectOffering().getId() : null,
                            sectionOptions
                    );
                })
                .toList();

        return new NextSemesterPlanOverview(
                target.semester().getId(),
                target.semester().getName(),
                target.academicYear(),
                target.semesterNumber(),
                true,
                MAX_SELECTIONS,
                savedSelections.size(),
                "Choose up to %d subjects for the next semester and save your preferred section times.".formatted(MAX_SELECTIONS),
                subjectOptions,
                savedSelections
        );
    }

    @Transactional
    public PlanActionResult selectSection(Student student, Long sectionId) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));
        SubjectOffering offering = subjectOfferingRepository.findByIdWithDetails(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (offering.getSemester() == null || !Objects.equals(offering.getSemester().getId(), target.semester().getId())) {
            throw new IllegalArgumentException("Section is not part of the target next semester");
        }

        List<ProgramCurriculumItem> curriculumItems = programCurriculumItemRepository
                .findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(
                        student.getProgram().getId(),
                        target.academicYear(),
                        target.semesterNumber()
                );
        boolean allowedSubject = curriculumItems.stream()
                .anyMatch(item -> Objects.equals(item.getSubject().getId(), offering.getSubject().getId()));
        if (!allowedSubject) {
            throw new IllegalArgumentException("Section is not part of the recommended next-semester curriculum");
        }

        List<PlannedRegistration> selections = plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(student.getId(), target.semester().getId());
        Optional<PlannedRegistration> sameOffering = selections.stream()
                .filter(selection -> Objects.equals(selection.getSubjectOffering().getId(), offering.getId()))
                .findFirst();
        if (sameOffering.isPresent()) {
            return new PlanActionResult(true, "Section is already saved in your next-semester plan.");
        }

        Optional<PlannedRegistration> sameSubject = selections.stream()
                .filter(selection -> selection.getSubjectOffering() != null && selection.getSubjectOffering().getSubject() != null)
                .filter(selection -> Objects.equals(selection.getSubjectOffering().getSubject().getId(), offering.getSubject().getId()))
                .findFirst();

        List<PlannedRegistration> competingSelections = selections.stream()
                .filter(selection -> sameSubject.isEmpty() || !Objects.equals(selection.getId(), sameSubject.get().getId()))
                .toList();

        if (sameSubject.isEmpty() && competingSelections.size() >= MAX_SELECTIONS) {
            throw new IllegalArgumentException("You can save no more than %d subjects for the next semester.".formatted(MAX_SELECTIONS));
        }

        List<String> reasons = evaluateSelection(student, target, offering, competingSelections);
        if (!reasons.isEmpty()) {
            throw new IllegalArgumentException(reasons.get(0));
        }

        sameSubject.ifPresent(plannedRegistrationRepository::delete);

        PlannedRegistration selection = plannedRegistrationRepository.save(PlannedRegistration.builder()
                .student(student)
                .semester(target.semester())
                .subjectOffering(offering)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        auditService.logStudentAction(
                student,
                "NEXT_SEMESTER_SELECTION_SAVED",
                "PlannedRegistration",
                selection.getId(),
                "sectionId=" + sectionId
        );
        return new PlanActionResult(true, "Next-semester section saved.");
    }

    @Transactional
    public PlanActionResult removeSection(Student student, Long sectionId) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));
        PlannedRegistration selection = plannedRegistrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Saved next-semester section not found"));
        if (!Objects.equals(selection.getSemester().getId(), target.semester().getId())) {
            throw new IllegalArgumentException("Saved section is not part of the active next-semester plan");
        }

        plannedRegistrationRepository.deleteByStudentIdAndSubjectOfferingId(student.getId(), sectionId);
        auditService.logStudentAction(
                student,
                "NEXT_SEMESTER_SELECTION_REMOVED",
                "PlannedRegistration",
                selection.getId(),
                "sectionId=" + sectionId
        );
        return new PlanActionResult(true, "Next-semester section removed.");
    }

    private List<String> evaluateSelection(
            Student student,
            PlanningTarget target,
            SubjectOffering offering,
            List<PlannedRegistration> currentSelections
    ) {
        List<String> reasons = new ArrayList<>();

        if (student.getProgram() == null) {
            reasons.add("Student program is not configured");
            return reasons;
        }

        if (offering.getSubject() == null) {
            reasons.add("Section subject is missing");
            return reasons;
        }

        List<ProgramCurriculumItem> curriculumItems = programCurriculumItemRepository
                .findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(
                        student.getProgram().getId(),
                        target.academicYear(),
                        target.semesterNumber()
                );
        boolean allowedSubject = curriculumItems.stream()
                .anyMatch(item -> Objects.equals(item.getSubject().getId(), offering.getSubject().getId()));
        if (!allowedSubject) {
            reasons.add("Section does not belong to your next-semester curriculum");
        }

        boolean duplicateSubject = currentSelections.stream()
                .anyMatch(selection -> selection.getSubjectOffering() != null
                        && selection.getSubjectOffering().getSubject() != null
                        && Objects.equals(selection.getSubjectOffering().getSubject().getId(), offering.getSubject().getId()));
        if (duplicateSubject) {
            reasons.add("Another section for this subject is already saved");
        }

        if (hasScheduleConflict(currentSelections, offering)) {
            reasons.add("Saved next-semester sections would conflict in time");
        }

        return reasons;
    }

    private boolean hasScheduleConflict(List<PlannedRegistration> currentSelections, SubjectOffering candidate) {
        List<MeetingTime> candidateTimes = candidate.getMeetingTimes() != null ? candidate.getMeetingTimes() : List.of();
        if (candidateTimes.isEmpty()) {
            return false;
        }

        for (PlannedRegistration selection : currentSelections) {
            SubjectOffering existing = selection.getSubjectOffering();
            if (existing == null || Objects.equals(existing.getId(), candidate.getId())) {
                continue;
            }
            List<MeetingTime> existingTimes = existing.getMeetingTimes() != null ? existing.getMeetingTimes() : List.of();
            for (MeetingTime existingSlot : existingTimes) {
                for (MeetingTime candidateSlot : candidateTimes) {
                    if (existingSlot.getDayOfWeek() == candidateSlot.getDayOfWeek()
                            && existingSlot.getStartTime().isBefore(candidateSlot.getEndTime())
                            && candidateSlot.getStartTime().isBefore(existingSlot.getEndTime())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Optional<PlanningTarget> resolvePlanningTarget(Student student) {
        if (student.getProgram() == null || student.getCurrentSemester() == null) {
            return Optional.empty();
        }

        String currentSemesterName = student.getCurrentSemester().getName() != null
                ? student.getCurrentSemester().getName().toLowerCase(Locale.ROOT)
                : "";
        boolean currentIsSpring = currentSemesterName.contains("spring");
        int nextAcademicYear = currentIsSpring ? student.getCourse() + 1 : student.getCourse();
        int nextSemesterNumber = currentIsSpring ? 1 : 2;
        if (nextAcademicYear > 4) {
            return Optional.empty();
        }

        Semester targetSemester = semesterRepository.findAll().stream()
                .filter(semester -> semester.getStartDate() != null && student.getCurrentSemester().getStartDate() != null)
                .filter(semester -> semester.getStartDate().isAfter(student.getCurrentSemester().getStartDate()))
                .sorted(Comparator.comparing(Semester::getStartDate))
                .findFirst()
                .orElse(null);
        if (targetSemester == null) {
            return Optional.empty();
        }

        return Optional.of(new PlanningTarget(targetSemester, nextAcademicYear, nextSemesterNumber));
    }

    private List<MeetingTimeDto> buildMeetingTimes(SubjectOffering offering) {
        if (offering.getMeetingTimes() == null) {
            return List.of();
        }
        return offering.getMeetingTimes().stream()
                .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                .map(slot -> new MeetingTimeDto(
                        slot.getDayOfWeek().name(),
                        slot.getStartTime().toString(),
                        slot.getEndTime().toString(),
                        slot.getRoom(),
                        slot.getLessonType() != null ? slot.getLessonType().name() : null
                ))
                .toList();
    }

    private NextSemesterSelectionDto toSelectionDto(PlannedRegistration selection) {
        SubjectOffering offering = selection.getSubjectOffering();
        return new NextSemesterSelectionDto(
                selection.getId(),
                offering.getId(),
                offering.getSubject().getCode(),
                offering.getSubject().getName(),
                offering.getSubject().getCredits(),
                offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                buildMeetingTimes(offering)
        );
    }

    private record PlanningTarget(Semester semester, int academicYear, int semesterNumber) {}

    public record NextSemesterPlanOverview(
            Long semesterId,
            String semesterName,
            Integer academicYear,
            Integer semesterNumber,
            boolean selectionEnabled,
            int maxSelections,
            int selectedCount,
            String message,
            List<NextSemesterSubjectOptionDto> subjects,
            List<NextSemesterSelectionDto> savedSelections
    ) {}

    public record NextSemesterSubjectOptionDto(
            Long subjectId,
            String subjectCode,
            String subjectName,
            int credits,
            boolean required,
            int displayOrder,
            Long selectedSectionId,
            List<NextSemesterSectionOptionDto> sections
    ) {}

    public record NextSemesterSectionOptionDto(
            Long sectionId,
            String teacherName,
            int capacity,
            long occupiedSeats,
            List<MeetingTimeDto> meetingTimes,
            boolean selected,
            List<String> blockedReasons,
            boolean canSelect
    ) {}

    public record NextSemesterSelectionDto(
            Long plannedRegistrationId,
            Long sectionId,
            String subjectCode,
            String subjectName,
            int credits,
            String teacherName,
            List<MeetingTimeDto> meetingTimes
    ) {}

    public record MeetingTimeDto(
            String dayOfWeek,
            String startTime,
            String endTime,
            String room,
            String lessonType
    ) {}

    public record PlanActionResult(boolean success, String message) {}
}
