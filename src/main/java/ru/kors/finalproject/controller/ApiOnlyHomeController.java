package ru.kors.finalproject.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ApiOnlyHomeController {

    private final String frontendUrl;

    public ApiOnlyHomeController(
            @Value("${app.web.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping({
            "/", "/home",
            "/login", "/register", "/news",
            "/professors", "/professors/{*path}",
            "/app", "/app/{*path}"
    })
    public String redirectToFrontend(HttpServletRequest request) {
        String query = request.getQueryString();
        String suffix = query == null || query.isBlank() ? "" : "?" + query;
        return "redirect:" + frontendUrl + request.getRequestURI() + suffix;
    }
}
