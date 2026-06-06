package com.matrix.cloud.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "meeting_summaries")
public class MeetingSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "original_text", columnDefinition = "LONGTEXT", nullable = false)
    private String originalText;

    @Column(name = "summary_text", columnDefinition = "LONGTEXT")
    private String summaryText;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PROCESSING;

    @Column(name = "minio_object_name", length = 255)
    private String minioObjectName;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        PROCESSING, COMPLETED, FAILED
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
