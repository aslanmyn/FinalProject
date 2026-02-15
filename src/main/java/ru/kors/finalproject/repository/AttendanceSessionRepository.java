package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.AttendanceSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findBySubjectOfferingIdOrderByClassDateDesc(Long subjectOfferingId);

    Optional<AttendanceSession> findBySubjectOfferingIdAndClassDate(Long subjectOfferingId, LocalDate classDate);
}
