package ru.kors.finalproject.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KBTU Portal API")
                        .version("v1")
                        .description("""
                                API documentation for the KBTU Portal.
                                
                                Main flow:
                                1. Authenticate via POST /api/v1/auth/login
                                2. Copy the access token from the response
                                3. Click Authorize in Swagger UI and paste the Bearer token
                                
                                The API is grouped by domain: auth, public pages, student modules, teacher modules, admin modules, campus life, chat, and files.
                                """)
                        .contact(new Contact().name("KBTU Portal")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT from POST /api/v1/auth/login")));
    }
}
