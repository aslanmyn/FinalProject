package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Attendance;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    List<Attendance> findBySubjectOfferingIdAndDate(Long subjectOfferingId, java.time.LocalDate date);

    List<Attendance> findBySubjectOfferingIdOrderByDateDesc(Long subjectOfferingId);

    @Query("SELECT a FROM Attendance a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.subjectOffering LEFT JOIN FETCH a.session WHERE a.subjectOffering.id = :offeringId ORDER BY a.date DESC")
    List<Attendance> findBySubjectOfferingIdOrderByDateDescWithDetails(Long offeringId);

    List<Attendance> findByStudentId(Long studentId);

    @Query("SELECT a FROM Attendance a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.subjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH a.session WHERE a.student.id = :studentId ORDER BY a.date DESC")
    List<Attendance> findByStudentIdWithDetails(Long studentId);
}
