package ru.kors.finalproject.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.kors.finalproject.entity.ChecklistTemplate;

import java.util.List;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, Long> {

    List<ChecklistTemplate> findByTriggerEventAndActiveTrue(ChecklistTemplate.TriggerEvent triggerEvent);

    List<ChecklistTemplate> findByActiveTrue();
}
