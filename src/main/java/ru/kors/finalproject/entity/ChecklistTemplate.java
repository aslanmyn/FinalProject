package ru.kors.finalproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checklist_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String linkToSection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TriggerEvent triggerEvent = TriggerEvent.ENROLLMENT;

    private int offsetDays;

    @Builder.Default
    private boolean active = true;

    public enum TriggerEvent {
        ENROLLMENT,
        SEMESTER_START,
        SEMESTER_END,
        CLEARANCE_START
    }
}
