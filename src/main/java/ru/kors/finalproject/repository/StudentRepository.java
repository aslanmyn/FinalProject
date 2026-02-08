package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Student;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);

    @Query("SELECT s FROM Student s LEFT JOIN FETCH s.program LEFT JOIN FETCH s.faculty LEFT JOIN FETCH s.currentSemester WHERE s.email = :email")
    Optional<Student> findByEmailWithDetails(String email);
}
