package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByProgramId(Long programId);
    Optional<Subject> findByCode(String code);

    List<Subject> findAllByOrderByCodeAsc();

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.program p LEFT JOIN FETCH p.faculty WHERE s.id = :id")
    Optional<Subject> findByIdWithProgram(Long id);

    @Query("SELECT s FROM Subject s LEFT JOIN FETCH s.program p LEFT JOIN FETCH p.faculty ORDER BY s.code ASC")
    List<Subject> findAllWithProgramOrderByCodeAsc();
}
