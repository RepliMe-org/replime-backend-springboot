package com.example.demo.entities;

import com.example.demo.entities.utils.Formality;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    private Chatbot chatbot;

    @NotNull(message = "Name cannot be null")
    private String name;

    @NotNull(message = "Description cannot be null")
    private String description;

    @NotNull(message = "greeting message cannot be null")
    private String greetingMessage;

    private boolean talkLikeMe;

    @Enumerated(EnumType.STRING)
    private Tone tone;

    @Enumerated(EnumType.STRING)
    private Verbosity verbosity;

    @Enumerated(EnumType.STRING)
    private Formality formality;

//    @Column(columnDefinition = "TEXT")
//    private String systemPrompt;

    private LocalDateTime createdAt;

    @PrePersist
    @PreUpdate
    private void validateAttributes() {
        if (talkLikeMe) {
            tone = null;
            verbosity = null;
            formality = null;
        } else {
            if (tone == null || verbosity == null || formality == null) {
                throw new IllegalStateException("Tone, verbosity, and formality must have a value when talkLikeMe is false");
            }
        }
    }
}
