package ru.kors.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final WebSessionFilter webSessionFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/**")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/files/download/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/teacher/**").hasRole("PROFESSOR")
                .requestMatchers("/api/v1/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(loginRateLimitFilter, AuthorizationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login", "/register", "/do-login", "/do-register", "/logout",
                    "/news", "/professors", "/professors/**",
                    "/css/**", "/js/**", "/images/**",
                    "/h2-console/**",
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/portal/**").hasRole("STUDENT")
                .requestMatchers("/professor/**").hasRole("PROFESSOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Legacy session APIs are disabled in favor of /api/v1/**
                .requestMatchers("/api/admin/**", "/api/professor/**", "/api/student/**").denyAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(loginRateLimitFilter, AuthorizationFilter.class)
            .addFilterBefore(webSessionFilter, AuthorizationFilter.class)
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**", "/h2-console/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }
}
