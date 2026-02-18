package ru.kors.finalproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Defence-in-depth filter that blocks unauthenticated/wrong-role access
 * to protected web areas (/admin/**, /portal/**, /professor/**).
 * <p>
 * This runs BEFORE controllers, so even if a controller forgets its
 * manual session check the request is still rejected.
 */
@Component
public class WebSessionFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/admin/")
                && !path.startsWith("/portal/")
                && !path.startsWith("/professor/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String role = session != null ? (String) session.getAttribute("userRole") : null;
        String email = session != null ? (String) session.getAttribute("userEmail") : null;

        if (role == null) {
            response.sendRedirect("/login");
            return;
        }

        var existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (email != null && (existingAuth == null || existingAuth instanceof AnonymousAuthenticationToken)) {
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(email, null, authorities)
            );
        }

        String path = request.getRequestURI();

        if (path.startsWith("/admin/") && !"ADMIN".equals(role)) {
            reject(response);
            return;
        }
        if (path.startsWith("/professor/") && !"PROFESSOR".equals(role)) {
            reject(response);
            return;
        }
        if (path.startsWith("/portal/") && !"STUDENT".equals(role)) {
            reject(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.sendRedirect("/login");
    }
}
