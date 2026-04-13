package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.MeetingTime;
import ru.kors.finalproject.entity.PlannedRegistration;
import ru.kors.finalproject.entity.ProgramCurriculumItem;
import ru.kors.finalproject.entity.Semester;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.Subject;
import ru.kors.finalproject.entity.SubjectOffering;
import ru.kors.finalproject.repository.PlannedRegistrationRepository;
import ru.kors.finalproject.repository.ProgramCurriculumItemRepository;
import ru.kors.finalproject.repository.SemesterRepository;
import ru.kors.finalproject.repository.SubjectOfferingRepository;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
                ).stream()
                .sorted(Comparator.comparingInt(ProgramCurriculumItem::getDisplayOrder))
                .toList();

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
        Map<Long, PlannedRegistration> selectedBySubjectId = selections.stream()
                .filter(selection -> selection.getSubject() != null)
                .collect(Collectors.toMap(
                        selection -> selection.getSubject().getId(),
                        selection -> selection,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<Long, Integer> displayOrderBySubjectId = curriculumItems.stream()
                .collect(Collectors.toMap(
                        item -> item.getSubject().getId(),
                        ProgramCurriculumItem::getDisplayOrder,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<NextSemesterSavedSubjectDto> savedSubjects = selections.stream()
                .sorted(Comparator
                        .comparing((PlannedRegistration selection) -> displayOrderBySubjectId.getOrDefault(selection.getSubject().getId(), Integer.MAX_VALUE))
                        .thenComparing(selection -> selection.getSubject().getCode(), Comparator.nullsLast(String::compareTo)))
                .map(this::toSavedSubjectDto)
                .toList();

        List<NextSemesterSubjectOptionDto> subjectOptions = curriculumItems.stream()
                .map(item -> {
                    PlannedRegistration savedSubject = selectedBySubjectId.get(item.getSubject().getId());
                    boolean saved = savedSubject != null;
                    Long selectedSectionId = savedSubject != null && savedSubject.getSubjectOffering() != null
                            ? savedSubject.getSubjectOffering().getId()
                            : null;

                    List<SubjectOffering> subjectOfferings = offerings.stream()
                            .filter(offering -> Objects.equals(offering.getSubject().getId(), item.getSubject().getId()))
                            .toList();

                    List<NextSemesterSectionOptionDto> sectionOptions = subjectOfferings.stream()
                            .map(offering -> {
                                boolean selected = Objects.equals(selectedSectionId, offering.getId());
                                List<String> reasons;
                                boolean canSelect;
                                if (!saved) {
                                    reasons = List.of("Save this subject first, then choose a preferred section time.");
                                    canSelect = false;
                                } else if (selected) {
                                    reasons = List.of();
                                    canSelect = true;
                                } else {
                                    reasons = evaluateSectionSelection(student, target, item.getSubject(), offering, selections);
                                    canSelect = reasons.isEmpty();
                                }
                                return new NextSemesterSectionOptionDto(
                                        offering.getId(),
                                        offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                                        offering.getCapacity(),
                                        plannedRegistrationRepository.countBySubjectOfferingId(offering.getId()),
                                        buildMeetingTimes(offering),
                                        selected,
                                        reasons,
                                        canSelect
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
                            saved,
                            selectedSectionId,
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
                savedSubjects.size(),
                "Save up to %d subjects for the next semester. After that, choose your preferred section times.".formatted(MAX_SELECTIONS),
                subjectOptions,
                savedSubjects
        );
    }

    @Transactional
    public PlanActionResult saveSubject(Student student, Long subjectId) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));

        ProgramCurriculumItem curriculumItem = programCurriculumItemRepository
                .findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(
                        student.getProgram().getId(),
                        target.academicYear(),
                        target.semesterNumber()
                ).stream()
                .filter(item -> Objects.equals(item.getSubject().getId(), subjectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Subject is not part of the recommended next-semester curriculum"));

        Optional<PlannedRegistration> existing = plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(
                student.getId(),
                target.semester().getId(),
                subjectId
        );
        if (existing.isPresent()) {
            return new PlanActionResult(true, "Subject is already saved for your next-semester plan.");
        }

        if (plannedRegistrationRepository.countByStudentIdAndSemesterId(student.getId(), target.semester().getId()) >= MAX_SELECTIONS) {
            throw new IllegalArgumentException("You can save no more than %d subjects for the next semester.".formatted(MAX_SELECTIONS));
        }

        PlannedRegistration selection = plannedRegistrationRepository.save(PlannedRegistration.builder()
                .student(student)
                .semester(target.semester())
                .subject(curriculumItem.getSubject())
                .subjectOffering(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        auditService.logStudentAction(
                student,
                "NEXT_SEMESTER_SUBJECT_SAVED",
                "PlannedRegistration",
                selection.getId(),
                "subjectId=" + subjectId
        );
        return new PlanActionResult(true, "Subject saved. You can choose a preferred section time later.");
    }

    @Transactional
    public PlanActionResult removeSubject(Student student, Long subjectId) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));

        PlannedRegistration selection = plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(
                        student.getId(),
                        target.semester().getId(),
                        subjectId
                )
                .orElseThrow(() -> new IllegalArgumentException("Saved next-semester subject not found"));

        plannedRegistrationRepository.deleteByStudentIdAndSemesterIdAndSubjectId(student.getId(), target.semester().getId(), subjectId);
        auditService.logStudentAction(
                student,
                "NEXT_SEMESTER_SUBJECT_REMOVED",
                "PlannedRegistration",
                selection.getId(),
                "subjectId=" + subjectId
        );
        return new PlanActionResult(true, "Subject removed from your next-semester plan.");
    }

    @Transactional
    public PlanActionResult chooseSection(Student student, Long subjectId, Long sectionId) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));

        PlannedRegistration selection = plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(
                        student.getId(),
                        target.semester().getId(),
                        subjectId
                )
                .orElseThrow(() -> new IllegalArgumentException("Save the subject first before choosing a section"));

        SubjectOffering offering = subjectOfferingRepository.findByIdWithDetails(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        if (offering.getSemester() == null || !Objects.equals(offering.getSemester().getId(), target.semester().getId())) {
            throw new IllegalArgumentException("Section is not part of the target next semester");
        }
        if (offering.getSubject() == null || !Objects.equals(offering.getSubject().getId(), subjectId)) {
            throw new IllegalArgumentException("Section does not belong to the selected subject");
        }

        List<PlannedRegistration> currentSelections = plannedRegistrationRepository.findByStudentIdAndSemesterIdWithDetails(student.getId(), target.semester().getId());
        List<String> reasons = evaluateSectionSelection(student, target, selection.getSubject(), offering, currentSelections);
        if (!reasons.isEmpty()) {
            throw new IllegalArgumentException(reasons.get(0));
        }

        selection.setSubjectOffering(offering);
        selection.setUpdatedAt(Instant.now());
        plannedRegistrationRepository.save(selection);

        auditService.logStudentAction(
                student,
                "NEXT_SEMESTER_SECTION_SELECTED",
                "PlannedRegistration",
                selection.getId(),
                "subjectId=" + subjectId + ",sectionId=" + sectionId
        );
        return new PlanActionResult(true, "Preferred section saved for the selected subject.");
    }

    @Transactional
    public PlanActionResult clearSection(Student student, Long subjectId) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));

        PlannedRegistration selection = plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(
                        student.getId(),
                        target.semester().getId(),
                        subjectId
                )
                .orElseThrow(() -> new IllegalArgumentException("Saved next-semester subject not found"));

        if (selection.getSubjectOffering() == null) {
            return new PlanActionResult(true, "No preferred section was selected for this subject.");
        }

        Long previousSectionId = selection.getSubjectOffering().getId();
        selection.setSubjectOffering(null);
        selection.setUpdatedAt(Instant.now());
        plannedRegistrationRepository.save(selection);

        auditService.logStudentAction(
                student,
                "NEXT_SEMESTER_SECTION_CLEARED",
                "PlannedRegistration",
                selection.getId(),
                "subjectId=" + subjectId + ",sectionId=" + previousSectionId
        );
        return new PlanActionResult(true, "Preferred section cleared. The subject remains in your next-semester plan.");
    }

    @Transactional
    public PlanActionResult legacySaveSection(Student student, Long sectionId) {
        SubjectOffering offering = subjectOfferingRepository.findByIdWithDetails(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        if (offering.getSubject() == null) {
            throw new IllegalArgumentException("Section subject is missing");
        }
        saveSubjectIfMissing(student, offering.getSubject());
        return chooseSection(student, offering.getSubject().getId(), sectionId);
    }

    @Transactional
    public PlanActionResult legacyRemoveSection(Student student, Long sectionId) {
        PlannedRegistration selection = plannedRegistrationRepository.findByStudentIdAndSubjectOfferingId(student.getId(), sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Saved next-semester section not found"));
        return removeSubject(student, selection.getSubject().getId());
    }

    private void saveSubjectIfMissing(Student student, Subject subject) {
        PlanningTarget target = resolvePlanningTarget(student)
                .orElseThrow(() -> new IllegalArgumentException("Next-semester planning is not available for this student"));
        boolean exists = plannedRegistrationRepository.findByStudentIdAndSemesterIdAndSubjectIdWithDetails(
                student.getId(),
                target.semester().getId(),
                subject.getId()
        ).isPresent();
        if (!exists) {
            saveSubject(student, subject.getId());
        }
    }

    private List<String> evaluateSectionSelection(
            Student student,
            PlanningTarget target,
            Subject subject,
            SubjectOffering offering,
            List<PlannedRegistration> currentSelections
    ) {
        List<String> reasons = new ArrayList<>();

        if (student.getProgram() == null) {
            reasons.add("Student program is not configured");
            return reasons;
        }

        if (subject == null || offering.getSubject() == null) {
            reasons.add("Section subject is missing");
            return reasons;
        }

        if (!Objects.equals(subject.getId(), offering.getSubject().getId())) {
            reasons.add("Section does not belong to the saved subject");
            return reasons;
        }

        boolean allowedSubject = programCurriculumItemRepository
                .findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(
                        student.getProgram().getId(),
                        target.academicYear(),
                        target.semesterNumber()
                ).stream()
                .anyMatch(item -> Objects.equals(item.getSubject().getId(), subject.getId()));
        if (!allowedSubject) {
            reasons.add("Section does not belong to your next-semester curriculum");
        }

        if (hasScheduleConflict(currentSelections, subject.getId(), offering)) {
            reasons.add("Preferred section would conflict with another selected section time.");
        }

        return reasons;
    }

    private boolean hasScheduleConflict(List<PlannedRegistration> currentSelections, Long subjectIdToIgnore, SubjectOffering candidate) {
        List<TimeSlot> candidateTimes = extractTimeSlots(candidate);
        if (candidateTimes.isEmpty()) {
            return false;
        }

        for (PlannedRegistration selection : currentSelections) {
            if (selection.getSubject() == null || Objects.equals(selection.getSubject().getId(), subjectIdToIgnore)) {
                continue;
            }
            SubjectOffering existing = selection.getSubjectOffering();
            if (existing == null) {
                continue;
            }
            List<TimeSlot> existingTimes = extractTimeSlots(existing);
            for (TimeSlot existingSlot : existingTimes) {
                for (TimeSlot candidateSlot : candidateTimes) {
                    if (existingSlot.dayOfWeek() == candidateSlot.dayOfWeek()
                            && existingSlot.startTime().isBefore(candidateSlot.endTime())
                            && candidateSlot.startTime().isBefore(existingSlot.endTime())) {
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
        return extractTimeSlots(offering).stream()
                .map(slot -> new MeetingTimeDto(
                        slot.dayOfWeek().name(),
                        slot.startTime().toString(),
                        slot.endTime().toString(),
                        slot.room(),
                        slot.lessonType()
                ))
                .toList();
    }

    private List<TimeSlot> extractTimeSlots(SubjectOffering offering) {
        if (offering == null) {
            return List.of();
        }
        if (offering.getMeetingTimes() != null && !offering.getMeetingTimes().isEmpty()) {
            return offering.getMeetingTimes().stream()
                    .sorted(Comparator.comparing(MeetingTime::getDayOfWeek).thenComparing(MeetingTime::getStartTime))
                    .map(slot -> new TimeSlot(
                            slot.getDayOfWeek(),
                            slot.getStartTime(),
                            slot.getEndTime(),
                            slot.getRoom(),
                            slot.getLessonType() != null ? slot.getLessonType().name() : null
                    ))
                    .toList();
        }
        if (offering.getDayOfWeek() == null || offering.getStartTime() == null || offering.getEndTime() == null) {
            return List.of();
        }
        return List.of(new TimeSlot(
                offering.getDayOfWeek(),
                offering.getStartTime(),
                offering.getEndTime(),
                offering.getRoom(),
                offering.getLessonType() != null ? offering.getLessonType().name() : null
        ));
    }

    private NextSemesterSavedSubjectDto toSavedSubjectDto(PlannedRegistration selection) {
        SubjectOffering offering = selection.getSubjectOffering();
        return new NextSemesterSavedSubjectDto(
                selection.getId(),
                selection.getSubject().getId(),
                selection.getSubject().getCode(),
                selection.getSubject().getName(),
                selection.getSubject().getCredits(),
                offering != null ? offering.getId() : null,
                offering != null && offering.getTeacher() != null ? offering.getTeacher().getName() : null,
                buildMeetingTimes(offering),
                offering != null
        );
    }

    private record TimeSlot(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String room,
            String lessonType
    ) {}

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
            List<NextSemesterSavedSubjectDto> savedSubjects
    ) {}

    public record NextSemesterSubjectOptionDto(
            Long subjectId,
            String subjectCode,
            String subjectName,
            int credits,
            boolean required,
            int displayOrder,
            boolean saved,
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

    public record NextSemesterSavedSubjectDto(
            Long plannedRegistrationId,
            Long subjectId,
            String subjectCode,
            String subjectName,
            int credits,
            Long selectedSectionId,
            String teacherName,
            List<MeetingTimeDto> meetingTimes,
            boolean sectionSelected
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
