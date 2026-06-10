package com.example.demo.entities;

import com.example.demo.entities.utils.FailedStage;
import com.example.demo.entities.utils.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private TrainingSource trainingSource;

    private String youtubeVideoId;

    private String title;

    private String duration;

    private Integer totalChunks;

    private String thumbnailUrl;

    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "failed_stage")
    private FailedStage failedStage;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_retry_at")
    private OffsetDateTime lastRetryAt;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    public boolean hasRetriesLeft() {
        return retryCount < maxRetries;
    }

    public boolean isPermanentlyFailed() {
        return syncStatus == SyncStatus.DEAD;
    }

    //exponential backoff strategy for retrying failed videos first 1min, sec: 4min, third: 9min
    public OffsetDateTime nextRetryEligibleAt() {
        long minutes = (long) Math.pow(retryCount, 2);
        return lastRetryAt != null
                ? lastRetryAt.plusMinutes(Math.max(minutes, 1))
                : OffsetDateTime.now();
    }
}