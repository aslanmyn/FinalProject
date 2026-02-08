package ru.kors.finalproject.entity.clearance;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checklist_item_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItemTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String linkToSection;
    private boolean mandatory;
}
