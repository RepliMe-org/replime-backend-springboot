package com.example.demo.entities;

import com.example.demo.entities.utils.SyncStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

}