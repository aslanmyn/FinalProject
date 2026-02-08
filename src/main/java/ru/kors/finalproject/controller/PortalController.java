package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kors.finalproject.model.PortalSection;
import ru.kors.finalproject.service.PortalDataService;
import ru.kors.finalproject.service.SessionService;

@Controller
@RequiredArgsConstructor
public class PortalController {

    private final PortalDataService portalDataService;
    private final SessionService sessionService;

    @GetMapping("/portal/{slug}")
    public String section(@PathVariable String slug, HttpSession session, Model model) {
        if ("news".equalsIgnoreCase(slug)) {
            return "redirect:/news";
        }
        PortalSection section = PortalSection.findBySlug(slug);
        if (section == null) {
            return "redirect:/news";
        }
        model.addAttribute("section", section);

        if (portalDataService.loadData(slug, session, model)) {
            return "portal/" + slug;
        }
        if (sessionService.getEmail(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("userEmail", sessionService.getEmail(session));
        model.addAttribute("userRole", sessionService.getRole(session));
        return "section";
    }
}
