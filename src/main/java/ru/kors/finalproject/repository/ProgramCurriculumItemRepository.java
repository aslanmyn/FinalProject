package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.ProgramCurriculumItem;

import java.util.List;

public interface ProgramCurriculumItemRepository extends JpaRepository<ProgramCurriculumItem, Long> {
    @Query("""
            SELECT pci
            FROM ProgramCurriculumItem pci
            LEFT JOIN FETCH pci.subject s
            LEFT JOIN FETCH pci.program p
            WHERE p.id = :programId
              AND pci.academicYear = :academicYear
              AND pci.semesterNumber = :semesterNumber
            ORDER BY pci.displayOrder ASC
            """)
    List<ProgramCurriculumItem> findByProgramIdAndAcademicYearAndSemesterNumberWithDetails(
            Long programId,
            int academicYear,
            int semesterNumber
    );
}
