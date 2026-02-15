package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.AssessmentComponent;

import java.util.List;

public interface AssessmentComponentRepository extends JpaRepository<AssessmentComponent, Long> {
    List<AssessmentComponent> findBySubjectOfferingIdOrderByCreatedAtAsc(Long subjectOfferingId);
}
