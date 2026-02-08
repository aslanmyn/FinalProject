package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.Faculty;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
}
