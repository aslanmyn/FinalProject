package ru.kors.finalproject.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "file_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String originalName;

    @Column(nullable = false)
    private String storagePath;

    private String contentType;

    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileCategory category;

    private String linkedEntityType;

    private Long linkedEntityId;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_student_id")
    private Student ownerStudent;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(nullable = false)
    private Instant uploadedAt;

    public enum FileCategory {
        STUDENT_FILE,
        REQUEST_ATTACHMENT,
        MOBILITY_DOCUMENT,
        PAYMENT_RECEIPT,
        TRANSCRIPT_PDF,
        OTHER
    }
}
