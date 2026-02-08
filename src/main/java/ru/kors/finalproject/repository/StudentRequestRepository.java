package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.StudentRequest;

import java.util.List;

public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {
    List<StudentRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
