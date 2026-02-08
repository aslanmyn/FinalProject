package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.Program;

public interface ProgramRepository extends JpaRepository<Program, Long> {
}
