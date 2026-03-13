package ru.kors.finalproject.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
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
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/files/download/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/teacher/**").hasRole("PROFESSOR")
                .requestMatchers("/api/v1/student/**").hasRole("STUDENT")
                .anyRequest().authenticated()
            )
            .addFilterBefore(loginRateLimitFilter, AuthorizationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "app.web", name = "legacy-enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain webLegacySecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login", "/register", "/do-login", "/do-register", "/logout",
                    "/news", "/professors", "/professors/**",
                    "/css/**", "/js/**", "/images/**",
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/portal/**").hasRole("STUDENT")
                .requestMatchers("/professor/**").hasRole("PROFESSOR")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .addFilterBefore(loginRateLimitFilter, AuthorizationFilter.class)
            .addFilterBefore(webSessionFilter, AuthorizationFilter.class)
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"));
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "app.web", name = "legacy-enabled", havingValue = "false")
    public SecurityFilterChain webApiOnlySecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/health", "/actuator/info",
                    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().permitAll()
            )
            .addFilterBefore(loginRateLimitFilter, AuthorizationFilter.class)
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .csrf(csrf -> csrf.disable());
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));
        return http.build();
    }
}
