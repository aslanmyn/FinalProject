package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.kors.finalproject.entity.GradeChangeRequest;

import java.util.List;

public interface GradeChangeRequestRepository extends JpaRepository<GradeChangeRequest, Long> {
    List<GradeChangeRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    Page<GradeChangeRequest> findByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);

    List<GradeChangeRequest> findBySubjectOfferingIdOrderByCreatedAtDesc(Long subjectOfferingId);

    List<GradeChangeRequest> findByStatusOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus status);

    Page<GradeChangeRequest> findByStatusOrderByCreatedAtDesc(GradeChangeRequest.RequestStatus status, Pageable pageable);
}
