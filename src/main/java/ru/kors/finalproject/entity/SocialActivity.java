package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "social_activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private String title;
    private String category;
    private int hours;
    private LocalDate date;
    private boolean confirmed;
}
