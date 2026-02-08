package ru.kors.finalproject.entity.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    public static final String ROLE_COORDINATOR = "ROLE_COORDINATOR";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
}
