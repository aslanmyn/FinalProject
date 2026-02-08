package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.StudentFile;

import java.util.List;

public interface StudentFileRepository extends JpaRepository<StudentFile, Long> {
    List<StudentFile> findByStudentIdOrderByUploadedAtDesc(Long studentId);
}
