package ru.kors.finalproject.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.kors.finalproject.service.UserRole;
import ru.kors.finalproject.service.UserRoleDetector;

@Controller
public class LoginController {

    private final UserRoleDetector roleDetector;

    public LoginController(UserRoleDetector roleDetector) {
        this.roleDetector = roleDetector;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @PostMapping("/do-login")
    public String doLogin(
            @RequestParam String email,
            @RequestParam(required = false) String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        UserRole role = roleDetector.detectRole(email);
        if (role == UserRole.UNKNOWN) {
            redirectAttributes.addFlashAttribute("error", "Invalid email format. Use student format (a_mustafayev@kbtu.kz) or teacher format (z.teacher@kbtu.kz)");
            return "redirect:/login";
        }
        session.setAttribute("userEmail", email);
        session.setAttribute("userRole", role.name());
        return "redirect:/news";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
