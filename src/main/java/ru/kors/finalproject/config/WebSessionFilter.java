package ru.kors.finalproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
                && !path.startsWith("/professor/")
                && !path.startsWith("/api/admin/")
                && !path.startsWith("/api/professor/")
                && !path.startsWith("/api/student/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String role = session != null ? (String) session.getAttribute("userRole") : null;

        if (role == null) {
            // No session → redirect browser, or 401 for AJAX/API
            if (isApiRequest(request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Not authenticated\"}");
                response.setContentType("application/json");
                return;
            }
            response.sendRedirect("/login");
            return;
        }

        String path = request.getRequestURI();

        if ((path.startsWith("/admin/") || path.startsWith("/api/admin/")) && !"ADMIN".equals(role)) {
            reject(request, response, "Access denied");
            return;
        }
        if ((path.startsWith("/professor/") || path.startsWith("/api/professor/")) && !"PROFESSOR".equals(role)) {
            reject(request, response, "Access denied");
            return;
        }
        if ((path.startsWith("/portal/") || path.startsWith("/api/student/")) && !"STUDENT".equals(role)) {
            reject(request, response, "Access denied");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String msg) throws IOException {
        if (isApiRequest(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + msg + "\"}");
        } else {
            response.sendRedirect("/login");
        }
    }
}
