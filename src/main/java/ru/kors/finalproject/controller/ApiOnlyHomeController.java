package ru.kors.finalproject.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnProperty(prefix = "app.web", name = "legacy-enabled", havingValue = "false")
public class ApiOnlyHomeController {

    private final String frontendUrl;

    public ApiOnlyHomeController(
            @Value("${app.web.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @GetMapping({"/", "/home"})
    public String home() {
        return "redirect:" + frontendUrl;
    }
}

