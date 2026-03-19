package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.AttendanceSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    @Query("""
            select distinct s from AttendanceSession s
            left join fetch s.subjectOffering so
            left join fetch so.subject
            left join fetch so.teacher
            left join fetch so.semester
            left join fetch so.meetingTimes
            where so.id = :subjectOfferingId
            order by s.classDate desc
            """)
    List<AttendanceSession> findBySubjectOfferingIdOrderByClassDateDesc(Long subjectOfferingId);

    @Query("""
            select s from AttendanceSession s
            left join fetch s.subjectOffering so
            left join fetch so.subject
            left join fetch so.teacher
            left join fetch so.semester
            left join fetch so.meetingTimes
            where so.id = :subjectOfferingId and s.classDate = :classDate
            """)
    Optional<AttendanceSession> findBySubjectOfferingIdAndClassDate(Long subjectOfferingId, LocalDate classDate);

    @Query("""
            select s from AttendanceSession s
            left join fetch s.subjectOffering so
            left join fetch so.subject
            left join fetch so.teacher
            left join fetch so.semester
            left join fetch so.meetingTimes
            where s.id = :sessionId
            """)
    Optional<AttendanceSession> findByIdWithDetails(Long sessionId);

    @Query("""
            select distinct s from AttendanceSession s
            left join fetch s.subjectOffering so
            left join fetch so.subject
            left join fetch so.teacher
            left join fetch so.semester
            left join fetch so.meetingTimes
            where so.id in :offeringIds and s.status = :status
            order by s.classDate desc
            """)
    List<AttendanceSession> findBySubjectOfferingIdInAndStatusWithDetails(List<Long> offeringIds, AttendanceSession.SessionStatus status);
}
