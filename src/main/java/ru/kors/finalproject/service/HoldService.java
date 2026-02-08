package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.user.StudentHold;
import ru.kors.finalproject.entity.user.StudentProfile;

import java.util.List;

/**
 * StudentHolds must be checked before registration, FX, and exams.
 */
@Service
@RequiredArgsConstructor
public class HoldService {

    public boolean hasActiveHold(StudentProfile student, StudentHold.HoldType type) {
        if (student == null || student.getHolds() == null) return false;
        return student.getHolds().stream()
                .anyMatch(h -> h.isActive() && h.getType() == type);
    }

    public boolean hasAnyActiveHold(StudentProfile student) {
        if (student == null || student.getHolds() == null) return false;
        return student.getHolds().stream().anyMatch(StudentHold::isActive);
    }

    public boolean canRegister(StudentProfile student) {
        return !hasActiveHold(student, StudentHold.HoldType.FINANCIAL)
                && !hasActiveHold(student, StudentHold.HoldType.ACADEMIC)
                && !hasActiveHold(student, StudentHold.HoldType.DOCUMENT);
    }

    public boolean canTakeExam(StudentProfile student) {
        return canRegister(student);
    }

    public List<StudentHold> getActiveHolds(StudentProfile student) {
        if (student == null || student.getHolds() == null) return List.of();
        return student.getHolds().stream().filter(StudentHold::isActive).toList();
    }
}
