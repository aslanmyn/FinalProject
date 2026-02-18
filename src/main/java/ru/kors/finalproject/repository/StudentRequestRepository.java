package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kors.finalproject.entity.StudentRequest;

import java.util.List;
import java.util.Optional;

public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {
    List<StudentRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT r FROM StudentRequest r LEFT JOIN FETCH r.student LEFT JOIN FETCH r.assignedTo WHERE r.student.id = :studentId ORDER BY r.createdAt DESC")
    List<StudentRequest> findByStudentIdWithDetailsOrderByCreatedAtDesc(Long studentId);

    @Query("SELECT r FROM StudentRequest r LEFT JOIN FETCH r.student LEFT JOIN FETCH r.assignedTo WHERE r.id = :id")
    Optional<StudentRequest> findByIdWithDetails(Long id);

    Page<StudentRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
    Page<StudentRequest> findByStudentId(Long studentId, Pageable pageable);

    List<StudentRequest> findByAssignedToIdOrderByCreatedAtDesc(Long assignedToId);

    List<StudentRequest> findByStatusOrderByCreatedAtDesc(StudentRequest.RequestStatus status);

    @Query("SELECT r FROM StudentRequest r LEFT JOIN FETCH r.student LEFT JOIN FETCH r.assignedTo WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<StudentRequest> findByStatusWithStudentOrderByCreatedAtDesc(StudentRequest.RequestStatus status);

    @Query("SELECT r FROM StudentRequest r LEFT JOIN FETCH r.student LEFT JOIN FETCH r.assignedTo ORDER BY r.createdAt DESC")
    List<StudentRequest> findAllWithDetails();
}
