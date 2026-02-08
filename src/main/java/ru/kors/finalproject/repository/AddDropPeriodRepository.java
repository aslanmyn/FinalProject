package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.AddDropPeriod;

import java.util.Optional;

public interface AddDropPeriodRepository extends JpaRepository<AddDropPeriod, Long> {
    Optional<AddDropPeriod> findBySemesterId(Long semesterId);
}
