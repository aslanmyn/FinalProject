package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Teacher;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByEmail(String email);

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.faculty WHERE t.email = :email")
    Optional<Teacher> findByEmailWithDetails(String email);

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.faculty WHERE t.id = :id")
    Optional<Teacher> findByIdWithDetails(Long id);

    List<Teacher> findAllByOrderByNameAsc();

    @Query("SELECT t FROM Teacher t LEFT JOIN FETCH t.faculty ORDER BY t.name ASC")
    List<Teacher> findAllWithFacultyOrderByNameAsc();
}
