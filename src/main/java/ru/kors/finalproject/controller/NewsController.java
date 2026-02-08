package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.kors.finalproject.model.PortalSection;
import ru.kors.finalproject.repository.NewsRepository;
import ru.kors.finalproject.service.SessionService;

@Controller
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepository;
    private final SessionService sessionService;

    @GetMapping("/news")
    public String news(HttpSession session, Model model) {
        model.addAttribute("userEmail", sessionService.getEmail(session) != null ? sessionService.getEmail(session) : "Guest");
        model.addAttribute("userRole", sessionService.getRole(session) != null ? sessionService.getRole(session) : "UNKNOWN");
        model.addAttribute("portalSections", PortalSection.ALL);
        model.addAttribute("newsList", newsRepository.findByOrderByCreatedAtDesc());
        return "news";
    }
}
