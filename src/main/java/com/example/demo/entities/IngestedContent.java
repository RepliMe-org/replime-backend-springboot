package com.example.demo.entities;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngestedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(columnDefinition = "TEXT")
    private String chunkTranscript;

    private String embeddingReference;

    private LocalDateTime processedAt;

    private Integer timestampStart;

    private Integer timestampEnd;

    private Integer chunkIndex;

//    @Column(columnDefinition = "JSON")
//    private String metadata;
}