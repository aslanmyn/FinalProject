package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.kors.finalproject.model.PortalSection;

@Controller
public class PortalController {

    @GetMapping("/portal/{slug}")
    public String section(@PathVariable String slug, HttpSession session, Model model) {
        if ("news".equalsIgnoreCase(slug)) {
            return "redirect:/news";
        }
        PortalSection section = PortalSection.findBySlug(slug);
        if (section == null) {
            return "redirect:/news";
        }
        String email = (String) session.getAttribute("userEmail");
        String role = (String) session.getAttribute("userRole");
        model.addAttribute("section", section);
        model.addAttribute("userEmail", email != null ? email : "Guest");
        model.addAttribute("userRole", role != null ? role : "UNKNOWN");
        return "section";
    }
}
