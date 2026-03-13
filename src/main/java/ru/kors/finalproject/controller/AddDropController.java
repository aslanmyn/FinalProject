package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kors.finalproject.config.LegacyWebEnabled;
import ru.kors.finalproject.service.AddDropService;
import ru.kors.finalproject.service.SessionService;

@LegacyWebEnabled
@Controller
@RequiredArgsConstructor
public class AddDropController {
    private final SessionService sessionService;
    private final AddDropService addDropService;

    @GetMapping("/portal/course-registration")
    public String registrationPage(HttpSession session, Model model) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return "redirect:/login";
        var s = student.get();
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("userRole", sessionService.getRole(session));
        model.addAttribute("student", s);
        model.addAttribute("registrations", addDropService.registrationsForStudent(s));
        model.addAttribute("available", addDropService.getAvailableForAdd(s));
        return "portal/course-registration";
    }

    @PostMapping("/portal/course-registration/submit")
    public String submitRegistration(@RequestParam Long subjectOfferingId, HttpSession session, RedirectAttributes ra) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return "redirect:/login";
        var result = addDropService.registerCourse(student.get(), subjectOfferingId);
        ra.addFlashAttribute("registrationMessage", result.message());
        ra.addFlashAttribute("registrationSuccess", result.success());
        return "redirect:/portal/course-registration";
    }

    @GetMapping("/portal/add-drop-courses")
    public String page(HttpSession session, Model model) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return "redirect:/login";
        var s = student.get();
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("userRole", sessionService.getRole(session));
        model.addAttribute("student", s);
        model.addAttribute("registrations", addDropService.registrationsForStudent(s));
        model.addAttribute("available", addDropService.getAvailableForAdd(s));
        return "portal/add-drop-courses";
    }

    @PostMapping("/portal/add-drop-courses/add")
    public String add(@RequestParam Long subjectOfferingId, HttpSession session, RedirectAttributes ra) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return "redirect:/login";
        var result = addDropService.addCourse(student.get(), subjectOfferingId);
        ra.addFlashAttribute("addDropMessage", result.message());
        ra.addFlashAttribute("addDropSuccess", result.success());
        return "redirect:/portal/add-drop-courses";
    }

    @PostMapping("/portal/add-drop-courses/drop")
    public String drop(@RequestParam Long subjectOfferingId, HttpSession session, RedirectAttributes ra) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return "redirect:/login";
        var result = addDropService.dropCourse(student.get(), subjectOfferingId);
        ra.addFlashAttribute("addDropMessage", result.message());
        ra.addFlashAttribute("addDropSuccess", result.success());
        return "redirect:/portal/add-drop-courses";
    }
}
