package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kors.finalproject.entity.StudentRequest;

import java.util.List;

public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {
    List<StudentRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    Page<StudentRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);

    List<StudentRequest> findByAssignedToIdOrderByCreatedAtDesc(Long assignedToId);

    List<StudentRequest> findByStatusOrderByCreatedAtDesc(StudentRequest.RequestStatus status);
}
