package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.Attendance;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    List<Attendance> findBySubjectOfferingIdAndDate(Long subjectOfferingId, java.time.LocalDate date);

    List<Attendance> findBySubjectOfferingIdOrderByDateDesc(Long subjectOfferingId);

    List<Attendance> findByStudentId(Long studentId);
}
