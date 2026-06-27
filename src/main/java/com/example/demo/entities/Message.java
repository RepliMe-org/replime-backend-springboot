package com.example.demo.entities;

import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.utils.MessageStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
@Table(name = "message", indexes = {
        @Index(name = "idx_message_session_id", columnList = "session_id"),
        @Index(name = "idx_message_session_sent", columnList = "session_id, sent_at ASC"),
        @Index(name = "idx_message_class_id", columnList = "message_class_id"),
        @Index(name = "idx_message_intent", columnList = "intent")
})
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @NotNull
    private ChatSession session;

    // Nullable — classification is async, assigned after the fact
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_class_id")
    private MessageClass messageClass;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageSender sender;

    @Enumerated(EnumType.STRING)
    private MessageIntent intent;

    private Boolean answeredWithSources;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MessageStatus status = MessageStatus.SENT;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    // Populated only for BOT messages to track AI processing time
    private LocalDateTime processedAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MessageSource> sources = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}
