package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id", nullable = false)
    private Chatbot chatbot;

    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    private String sourceValue;

    private LocalDateTime addedAt;

    @Enumerated(EnumType.STRING)
    private SyncStatus syncStatus;

    @Column(columnDefinition = "JSON")
    private String metadata;

    // Relationships
    @OneToMany(mappedBy = "trainingSource", cascade = CascadeType.ALL)
    private List<Video> videos = new ArrayList<>();
}