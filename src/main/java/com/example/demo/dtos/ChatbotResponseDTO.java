package com.example.demo.dtos;

import com.example.demo.entities.ChatbotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class ChatbotResponseDTO {

    @NotNull
    private UUID id;

    @NotNull
    private String influencerUsername;

    @NotNull
    private ChatbotStatus status;

    @NotNull
    private boolean isPublic;

    @NotNull
    private LocalDateTime createdAt;

    private String greetingMessage;

    // ChatbotConfig fields
    private UUID configId;

    private String chatbotName;

    private String chatbotDescription;

    private String systemPrompt;

    private String modelName;

    private Double temperature;

//    private Integer maxTokens;

    private Integer version;

    private boolean isActive;

    private LocalDateTime configCreatedAt;
}

