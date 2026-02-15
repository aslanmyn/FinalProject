package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.kors.finalproject.entity.RegistrationWindow;
import ru.kors.finalproject.repository.RegistrationWindowRepository;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WindowPolicyService {
    private final RegistrationWindowRepository registrationWindowRepository;

    public boolean isWindowActive(Long semesterId, RegistrationWindow.WindowType type) {
        LocalDate today = LocalDate.now();
        return registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(semesterId, type)
                .filter(w -> !today.isBefore(w.getStartDate()) && !today.isAfter(w.getEndDate()))
                .isPresent();
    }

    public Optional<RegistrationWindow> getActiveWindow(Long semesterId, RegistrationWindow.WindowType type) {
        LocalDate today = LocalDate.now();
        return registrationWindowRepository.findBySemesterIdAndTypeAndActiveTrue(semesterId, type)
                .filter(w -> !today.isBefore(w.getStartDate()) && !today.isAfter(w.getEndDate()));
    }
}
