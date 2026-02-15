package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.Grade;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findByStudentIdAndSubjectOffering_SemesterId(Long studentId, Long semesterId);

    List<Grade> findByStudentId(Long studentId);

    List<Grade> findBySubjectOfferingId(Long subjectOfferingId);

    List<Grade> findByStudentIdAndPublishedTrue(Long studentId);
}
