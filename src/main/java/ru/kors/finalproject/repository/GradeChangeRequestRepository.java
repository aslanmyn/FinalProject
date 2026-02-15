package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kors.finalproject.entity.GradeChangeRequest;

import java.util.List;

public interface GradeChangeRequestRepository extends JpaRepository<GradeChangeRequest, Long> {
    List<GradeChangeRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    @Query("SELECT g FROM GradeChangeRequest g " +
           "LEFT JOIN FETCH g.teacher " +
           "LEFT JOIN FETCH g.student " +
           "LEFT JOIN FETCH g.subjectOffering so LEFT JOIN FETCH so.subject " +
           "LEFT JOIN FETCH g.grade " +
           "WHERE g.teacher.id = :teacherId ORDER BY g.createdAt DESC")
    List<GradeChangeRequest> findByTeacherIdWithDetailsOrderByCreatedAtDesc(Long teacherId);

    Page<GradeChangeRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);

    List<GradeChangeRequest> findBySubjectOfferingIdOrderByCreatedAtDesc(Long subjectOfferingId);

    List<GradeChangeRequest> findByStatusOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus status);

    @Query("SELECT g FROM GradeChangeRequest g " +
           "LEFT JOIN FETCH g.teacher " +
           "LEFT JOIN FETCH g.student " +
           "LEFT JOIN FETCH g.subjectOffering so LEFT JOIN FETCH so.subject " +
           "WHERE g.status = :status ORDER BY g.createdAt DESC")
    List<GradeChangeRequest> findByStatusWithDetailsOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus status);

    Page<GradeChangeRequest> findByStatusOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus status, Pageable pageable);
}
