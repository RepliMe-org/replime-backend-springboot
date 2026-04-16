package com.example.demo.dtos;

import com.example.demo.entities.ChatbotStatus;
import com.example.demo.entities.Formality;
import com.example.demo.entities.Tone;
import com.example.demo.entities.Verbosity;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class InfluencerChatbotResponseDTO {

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

    @NotNull
    private String greetingMessage;

    // ChatbotConfig fields
    @NotNull
    private Long configId;

    @NotNull
    private String chatbotName;

    @NotNull
    private String chatbotDescription;

    @NotNull
    private boolean talkLikeMe;

    private Tone tone;

    private Verbosity verbosity;

    private Formality formality;

    @NotNull
    private LocalDateTime configCreatedAt;
}

