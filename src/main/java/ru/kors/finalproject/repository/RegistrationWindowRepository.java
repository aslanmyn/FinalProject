package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.RegistrationWindow;

import java.util.List;
import java.util.Optional;

public interface RegistrationWindowRepository extends JpaRepository<RegistrationWindow, Long> {
    Optional<RegistrationWindow> findBySemesterIdAndTypeAndActiveTrue(Long semesterId, RegistrationWindow.WindowType type);

    List<RegistrationWindow> findByTypeAndActiveTrue(RegistrationWindow.WindowType type);
}
