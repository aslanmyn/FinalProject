package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.kors.finalproject.service.SessionService;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final SessionService sessionService;

    @GetMapping("/")
    public String home(HttpSession session) {
        if (sessionService.isStudent(session)) {
            return "redirect:/news";
        }
        if (sessionService.isTeacher(session)) {
            return "redirect:/professor/dashboard";
        }
        if (sessionService.isAdmin(session)) {
            return "redirect:/admin/dashboard";
        }
        return "index";
    }
}
