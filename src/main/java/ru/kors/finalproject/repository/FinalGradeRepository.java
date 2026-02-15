package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.FinalGrade;

import java.util.List;
import java.util.Optional;

public interface FinalGradeRepository extends JpaRepository<FinalGrade, Long> {
    List<FinalGrade> findByStudentId(Long studentId);

    List<FinalGrade> findByStudentIdAndPublishedTrue(Long studentId);

    List<FinalGrade> findBySubjectOfferingId(Long subjectOfferingId);

    Optional<FinalGrade> findByStudentIdAndSubjectOfferingId(Long studentId, Long subjectOfferingId);
}
