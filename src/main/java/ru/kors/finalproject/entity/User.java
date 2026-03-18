package ru.kors.finalproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.security.Principal;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Principal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_admin_permissions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<AdminPermission> adminPermissions = EnumSet.noneOf(AdminPermission.class);

    private boolean enabled;

    public enum UserRole {
        ADMIN, PROFESSOR, STUDENT
    }

    public enum AdminPermission {
        SUPER,
        REGISTRAR,
        FINANCE,
        MOBILITY,
        SUPPORT,
        CONTENT
    }

    public boolean validatePassword(String rawPassword) {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches(rawPassword, this.password);
    }

    public boolean hasPermission(AdminPermission permission) {
        if (role != UserRole.ADMIN) {
            return false;
        }
        if (adminPermissions == null || adminPermissions.isEmpty()) {
            return false;
        }
        return adminPermissions.contains(AdminPermission.SUPER) || adminPermissions.contains(permission);
    }

    @Override
    public String getName() {
        return email;
    }
}
