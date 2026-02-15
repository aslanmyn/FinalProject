package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.AssessmentComponent;

import java.util.List;

public interface AssessmentComponentRepository extends JpaRepository<AssessmentComponent, Long> {
    List<AssessmentComponent> findBySubjectOfferingIdOrderByCreatedAtAsc(Long subjectOfferingId);

    @Query("SELECT c FROM AssessmentComponent c LEFT JOIN FETCH c.subjectOffering WHERE c.subjectOffering.id = :offeringId ORDER BY c.createdAt ASC")
    List<AssessmentComponent> findBySubjectOfferingIdWithDetailsOrderByCreatedAtAsc(Long offeringId);
}
