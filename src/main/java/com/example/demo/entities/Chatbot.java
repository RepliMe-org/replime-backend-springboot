package com.example.demo.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class Chatbot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(unique = true)
    @NotNull(message = "Influencer cannot be null")
    private User influencer;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Chatbot status cannot be null")
    private ChatbotStatus status;
    // DRAFT, CONFIGURING, TRAINING, ACTIVE, FAILED

    private boolean isPublic = false;

    @PastOrPresent(message = "Created date cannot be in the future")
    private LocalDateTime createdAt;

    @OneToOne
    private ChatbotConfig config;

    @ManyToOne(fetch = FetchType.LAZY)
    private ChatbotCategory category;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "custom_messageclass",
            joinColumns = @JoinColumn(name = "chatbot_id"),
            inverseJoinColumns = @JoinColumn(name = "message_class_id")
    )
    private Set<MessageClass> messageClasses = new HashSet<>();
}