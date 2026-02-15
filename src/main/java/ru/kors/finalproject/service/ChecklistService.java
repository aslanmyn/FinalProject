package ru.kors.finalproject.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.kors.finalproject.entity.ChecklistItem;
import ru.kors.finalproject.entity.ChecklistTemplate;
import ru.kors.finalproject.entity.Student;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.ChecklistItemRepository;
import ru.kors.finalproject.repository.ChecklistTemplateRepository;
import ru.kors.finalproject.repository.StudentRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChecklistService {

    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final StudentRepository studentRepository;
    private final AuditService auditService;

    public List<ChecklistTemplate> listTemplates() {
        return checklistTemplateRepository.findAll();
    }

    public List<ChecklistTemplate> listActiveTemplates() {
        return checklistTemplateRepository.findByActiveTrue();
    }

    @Transactional
    public ChecklistTemplate createTemplate(String title, String linkToSection,
                                             ChecklistTemplate.TriggerEvent triggerEvent,
                                             int offsetDays, User actor) {
        ChecklistTemplate template = ChecklistTemplate.builder()
                .title(title)
                .linkToSection(linkToSection)
                .triggerEvent(triggerEvent)
                .offsetDays(offsetDays)
                .active(true)
                .build();
        ChecklistTemplate saved = checklistTemplateRepository.save(template);
        auditService.logUserAction(actor, "CHECKLIST_TEMPLATE_CREATED", "ChecklistTemplate", saved.getId(),
                "title=" + title);
        return saved;
    }

    @Transactional
    public void deactivateTemplate(Long templateId, User actor) {
        ChecklistTemplate template = checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));
        template.setActive(false);
        checklistTemplateRepository.save(template);
        auditService.logUserAction(actor, "CHECKLIST_TEMPLATE_DEACTIVATED", "ChecklistTemplate",
                templateId, "title=" + template.getTitle());
    }

    @Transactional
    public void generateForStudent(Long studentId, ChecklistTemplate.TriggerEvent trigger, LocalDate baseDate) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        List<ChecklistTemplate> templates = checklistTemplateRepository.findByTriggerEventAndActiveTrue(trigger);
        for (ChecklistTemplate tpl : templates) {
            ChecklistItem item = ChecklistItem.builder()
                    .student(student)
                    .title(tpl.getTitle())
                    .linkToSection(tpl.getLinkToSection())
                    .deadline(baseDate.plusDays(tpl.getOffsetDays()))
                    .completed(false)
                    .build();
            checklistItemRepository.save(item);
        }
    }

    @Transactional
    public void generateForAllStudents(ChecklistTemplate.TriggerEvent trigger, LocalDate baseDate) {
        List<Student> students = studentRepository.findAll();
        for (Student student : students) {
            generateForStudent(student.getId(), trigger, baseDate);
        }
    }

    public List<ChecklistItem> listForStudent(Long studentId) {
        return checklistItemRepository.findByStudentIdOrderByDeadlineAsc(studentId);
    }
}
