package ru.kors.finalproject.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kors.finalproject.repository.SubjectOfferingRepository;
import ru.kors.finalproject.repository.TeacherRepository;
import ru.kors.finalproject.service.AnnouncementService;

@Controller
@RequiredArgsConstructor
public class PublicProfessorController {
    private final TeacherRepository teacherRepository;
    private final SubjectOfferingRepository subjectOfferingRepository;
    private final AnnouncementService announcementService;

    @GetMapping("/professors")
    public String professors(Model model) {
        model.addAttribute("professors", teacherRepository.findAllWithFacultyOrderByNameAsc());
        return "professors/list";
    }

    @GetMapping("/professors/{id}")
    public String professorProfile(@PathVariable Long id, Model model) {
        var professor = teacherRepository.findByIdWithDetails(id);
        if (professor.isEmpty()) {
            return "redirect:/professors";
        }
        var teacher = professor.get();
        model.addAttribute("teacher", teacher);
        model.addAttribute("currentSections", subjectOfferingRepository.findByTeacherIdWithDetails(teacher.getId()).stream()
                .filter(so -> so.getSemester() != null && so.getSemester().isCurrent())
                .toList());
        model.addAttribute("announcements", announcementService.listPublicByTeacher(teacher.getId()));
        return "professors/profile";
    }
}
