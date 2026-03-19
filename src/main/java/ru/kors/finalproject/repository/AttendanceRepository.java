package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);

    List<Attendance> findBySubjectOfferingIdAndDate(Long subjectOfferingId, LocalDate date);

    List<Attendance> findBySubjectOfferingIdOrderByDateDesc(Long subjectOfferingId);

    @Query("SELECT a FROM Attendance a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.subjectOffering LEFT JOIN FETCH a.session WHERE a.subjectOffering.id = :offeringId ORDER BY a.date DESC")
    List<Attendance> findBySubjectOfferingIdOrderByDateDescWithDetails(Long offeringId);

    List<Attendance> findByStudentId(Long studentId);

    @Query("SELECT a FROM Attendance a LEFT JOIN FETCH a.student LEFT JOIN FETCH a.subjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH a.session WHERE a.student.id = :studentId ORDER BY a.date DESC")
    List<Attendance> findByStudentIdWithDetails(Long studentId);

    @Query("""
            select a from Attendance a
            left join fetch a.student
            left join fetch a.subjectOffering so
            left join fetch so.subject
            left join fetch so.teacher
            left join fetch a.session
            where a.student.id = :studentId and a.subjectOffering.id = :offeringId and a.date = :date
            """)
    Optional<Attendance> findByStudentIdAndSubjectOfferingIdAndDateWithDetails(Long studentId, Long offeringId, LocalDate date);

    @Query("""
            select a from Attendance a
            left join fetch a.student
            left join fetch a.subjectOffering so
            left join fetch so.subject
            left join fetch so.teacher
            left join fetch a.session
            where a.session.id = :sessionId
            order by lower(a.student.name), a.student.id
            """)
    List<Attendance> findBySessionIdWithDetails(Long sessionId);
}
