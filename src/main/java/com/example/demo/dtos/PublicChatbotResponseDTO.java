package com.example.demo.dtos;

import com.example.demo.entities.ChatbotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class PublicChatbotResponseDTO {

    @NotNull
    private UUID id;

    @NotNull
    private String influencerUsername;

    @NotNull
    private String chatbotName;

    private String chatbotDescription;

    private String greetingMessage;

    @NotNull
    private ChatbotStatus status;
}

