package ru.kors.finalproject.entity.shared;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Shared storage entity across all modules.
 * Used by News, Requests, Mobility, etc.
 */
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

    private String originalFileName;
    private String storedPath;
    private String contentType;
    private long sizeBytes;
    private Instant uploadedAt;

    private String entityType; // e.g. "RequestTicket", "NewsPost", "MobilityApplication"
    private Long entityId;
}
