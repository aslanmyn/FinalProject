package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.kors.finalproject.model.PortalSection;
import ru.kors.finalproject.config.LegacyWebEnabled;
import ru.kors.finalproject.repository.NewsRepository;
import ru.kors.finalproject.service.AnnouncementService;
import ru.kors.finalproject.service.SessionService;

import java.util.List;

@LegacyWebEnabled
@Controller
@RequiredArgsConstructor
public class NewsController {

    private final NewsRepository newsRepository;
    private final SessionService sessionService;
    private final AnnouncementService announcementService;

    @GetMapping("/news")
    public String news(HttpSession session, Model model) {
        announcementService.publishScheduledAnnouncements();
        String userEmail = sessionService.getEmail(session) != null ? sessionService.getEmail(session) : "Guest";
        String role = sessionService.getRole(session) != null ? sessionService.getRole(session) : "UNKNOWN";

        model.addAttribute("userEmail", userEmail);
        model.addAttribute("userRole", role);
        model.addAttribute("isStudent", "STUDENT".equals(role));
        model.addAttribute("isProfessor", "PROFESSOR".equals(role));
        model.addAttribute("isAdmin", "ADMIN".equals(role));
        model.addAttribute("portalSections", "STUDENT".equals(role) ? PortalSection.ALL : List.of());
        model.addAttribute("newsList", newsRepository.findByOrderByCreatedAtDesc());
        return "news";
    }
}
