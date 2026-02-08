package ru.kors.finalproject.entity.user;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.academic.AcademicTerm;
import ru.kors.finalproject.entity.academic.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Student master data: personal and academic status.
 * One User → one StudentProfile (for students).
 */
@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String fullName;
    private int course;
    private String groupName;

    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private ru.kors.finalproject.entity.academic.Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_term_id")
    private AcademicTerm currentTerm;

    private int creditsEarned;
    private String passportNumber;
    private String address;
    private String phone;
    private String emergencyContact;

    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentHold> holds = new ArrayList<>();

    public enum StudentStatus { ACTIVE, ON_LEAVE, GRADUATED }
}
