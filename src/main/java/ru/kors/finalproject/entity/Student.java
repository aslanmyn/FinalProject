package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.kors.finalproject.entity.Faculty;

@Entity
@Table(name = "students", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String email;
    private String name;
    private int course;
    private String groupName;
    @Enumerated(EnumType.STRING)
    private StudentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_semester_id")
    private Semester currentSemester;

    private int creditsEarned;
    private String passportNumber;
    private String address;
    private String phone;
    private String emergencyContact;

    public enum StudentStatus { ACTIVE, ON_LEAVE, GRADUATED }
}
