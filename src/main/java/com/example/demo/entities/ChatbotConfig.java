package com.example.demo.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    private String name;
    private String description;

    private String greetingMessage;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

//    private String modelName;

    private Double temperature;

//    private Integer maxTokens;

//    private Integer version;

    private LocalDateTime createdAt;
}
