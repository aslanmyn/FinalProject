package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.CourseMaterial;

import java.util.List;

public interface CourseMaterialRepository extends JpaRepository<CourseMaterial, Long> {

    List<CourseMaterial> findBySubjectOfferingIdOrderByCreatedAtDesc(Long subjectOfferingId);

    List<CourseMaterial> findBySubjectOfferingIdAndPublishedTrueOrderByCreatedAtDesc(Long subjectOfferingId);

    List<CourseMaterial> findByUploadedByIdOrderByCreatedAtDesc(Long teacherId);
}
