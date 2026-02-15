package ru.kors.finalproject.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.kors.finalproject.entity.User;
import ru.kors.finalproject.repository.UserRepository;
import ru.kors.finalproject.service.JwtService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            var claims = jwtService.parse(token);

            Long userId;
            try {
                userId = Long.parseLong(claims.getSubject());
            } catch (NumberFormatException e) {
                filterChain.doFilter(request, response);
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isEnabled()) {
                filterChain.doFilter(request, response);
                return;
            }

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            if (user.getRole() == User.UserRole.ADMIN && user.getAdminPermissions() != null) {
                for (User.AdminPermission perm : user.getAdminPermissions()) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + perm.name()));
                }
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
        }

        filterChain.doFilter(request, response);
    }
}
