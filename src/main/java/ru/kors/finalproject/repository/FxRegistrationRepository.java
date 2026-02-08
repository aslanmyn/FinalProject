package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.FxRegistration;

import java.util.List;

public interface FxRegistrationRepository extends JpaRepository<FxRegistration, Long> {
    List<FxRegistration> findByStudentIdOrderByCreatedAtDesc(Long studentId);
}
