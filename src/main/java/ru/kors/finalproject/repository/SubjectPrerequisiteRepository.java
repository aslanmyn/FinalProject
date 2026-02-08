package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.SubjectPrerequisite;

import java.util.List;

public interface SubjectPrerequisiteRepository extends JpaRepository<SubjectPrerequisite, Long> {
    List<SubjectPrerequisite> findBySubjectId(Long subjectId);
}
