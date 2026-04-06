package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.Program;

import java.util.Optional;
import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    Optional<Program> findByName(String name);

    @Query("SELECT p FROM Program p LEFT JOIN FETCH p.faculty")
    List<Program> findAllWithFaculty();
}
