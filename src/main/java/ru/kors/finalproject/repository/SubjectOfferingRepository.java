package ru.kors.finalproject.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.SubjectOffering;

import java.util.List;
import java.util.Optional;

public interface SubjectOfferingRepository extends JpaRepository<SubjectOffering, Long> {
    List<SubjectOffering> findBySemesterId(Long semesterId);

    @Query("SELECT DISTINCT so FROM SubjectOffering so " +
           "LEFT JOIN FETCH so.subject " +
           "LEFT JOIN FETCH so.subject.program " +
           "LEFT JOIN FETCH so.semester " +
           "LEFT JOIN FETCH so.teacher " +
           "LEFT JOIN FETCH so.meetingTimes")
    List<SubjectOffering> findAllWithDetails();

    @Query("SELECT DISTINCT so FROM SubjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.subject.program LEFT JOIN FETCH so.semester LEFT JOIN FETCH so.teacher LEFT JOIN FETCH so.meetingTimes WHERE so.semester.id = :semesterId")
    List<SubjectOffering> findBySemesterIdWithDetails(Long semesterId);

    List<SubjectOffering> findByTeacherId(Long teacherId);

    @Query("SELECT so FROM SubjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.semester LEFT JOIN FETCH so.teacher WHERE so.teacher.id = :teacherId")
    List<SubjectOffering> findByTeacherIdWithDetails(Long teacherId);

    @Query("SELECT so FROM SubjectOffering so LEFT JOIN FETCH so.subject LEFT JOIN FETCH so.semester LEFT JOIN FETCH so.teacher WHERE so.id = :id")
    Optional<SubjectOffering> findByIdWithDetails(Long id);

    /** Lock the offering row to prevent concurrent enrollment race conditions. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT so FROM SubjectOffering so WHERE so.id = :id")
    Optional<SubjectOffering> findByIdForUpdate(Long id);
}
