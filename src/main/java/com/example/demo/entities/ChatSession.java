package com.example.demo.entities;

import com.example.demo.entities.utils.ChatSessionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "chat_session",
        indexes = {
                @Index(name = "idx_session_chatbot_id", columnList = "chatbot_id"),
                @Index(name = "idx_session_user_id", columnList = "user_id"),
                @Index(name = "idx_session_started_at", columnList = "started_at DESC"),
                @Index(name = "idx_session_chatbot_user", columnList = "chatbot_id, user_id")
        }
)
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chatbot_id", nullable = false)
    @NotNull(message = "Chatbot cannot be null")
    private Chatbot chatbot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ChatSessionStatus status = ChatSessionStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime startedAt;

    // Null until session is explicitly closed
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
    }
}