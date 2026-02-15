package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.CourseMaterial;

import java.util.List;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {

    List<CourseMaterial> findBySubjectOfferingIdOrderByCreatedAtDesc(Long subjectOfferingId);

    @Query("SELECT m FROM CourseMaterial m LEFT JOIN FETCH m.subjectOffering LEFT JOIN FETCH m.uploadedBy WHERE m.subjectOffering.id = :sectionId ORDER BY m.createdAt DESC")
    List<CourseMaterial> findBySubjectOfferingIdWithDetailsOrderByCreatedAtDesc(Long sectionId);

    List<CourseMaterial> findBySubjectOfferingIdAndPublishedTrueOrderByCreatedAtDesc(Long subjectOfferingId);

    List<CourseMaterial> findByUploadedByIdOrderByCreatedAtDesc(Long teacherId);
}
