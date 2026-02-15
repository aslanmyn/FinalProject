package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kors.finalproject.service.RequestService;
import ru.kors.finalproject.service.SessionService;

@Controller
@RequestMapping("/portal/student-requests")
@RequiredArgsConstructor
public class StudentRequestController {
    private final SessionService sessionService;
    private final RequestService requestService;

    @PostMapping("/create")
    public String create(
            @RequestParam String category,
            @RequestParam String description,
            HttpSession session,
            RedirectAttributes ra) {
        var student = sessionService.getCurrentStudent(session);
        if (student.isEmpty()) return "redirect:/login";
        requestService.createRequest(student.get(), category, description);
        ra.addFlashAttribute("requestCreated", true);
        return "redirect:/portal/student-requests";
    }
}
