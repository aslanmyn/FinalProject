package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.kors.finalproject.entity.RegistrationWindow;

import java.util.List;
import java.util.Optional;

public interface RegistrationWindowRepository extends JpaRepository<RegistrationWindow, Long> {
    Optional<RegistrationWindow> findBySemesterIdAndTypeAndActiveTrue(Long semesterId, RegistrationWindow.WindowType type);

    List<RegistrationWindow> findByTypeAndActiveTrue(RegistrationWindow.WindowType type);

    List<RegistrationWindow> findBySemesterIdOrderByStartDateAsc(Long semesterId);

    @Query("SELECT rw FROM RegistrationWindow rw LEFT JOIN FETCH rw.semester ORDER BY rw.startDate DESC")
    List<RegistrationWindow> findAllWithSemesterOrderByStartDateDesc();
}
