package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.MobilityApplication;

import java.util.List;

public interface MobilityApplicationRepository extends JpaRepository<MobilityApplication, Long> {
    List<MobilityApplication> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
