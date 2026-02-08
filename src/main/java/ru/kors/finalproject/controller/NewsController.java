package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NewsController {

    @GetMapping("/news")
    public String news(HttpSession session, Model model) {
        String email = (String) session.getAttribute("userEmail");
        String role = (String) session.getAttribute("userRole");
        model.addAttribute("userEmail", email != null ? email : "Guest");
        model.addAttribute("userRole", role != null ? role : "UNKNOWN");
        return "news";
    }
}
