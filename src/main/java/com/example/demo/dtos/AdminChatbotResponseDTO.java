package com.example.demo.dtos;

import com.example.demo.entities.utils.ChatbotStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AdminChatbotResponseDTO {
    @NotNull
    private UUID id;

    @NotNull
    private String chatbotName;

    @NotNull
    private String chatbotCategory;

    private String avatarUrl;


    @NotNull
    private int numberOfIngestedVideos;

    @NotNull
    private String channelHandle;

    @NotNull
    private String influencerUsername;

    private String chatbotDescription;

    private String greetingMessage;

    @NotNull
    private ChatbotStatus status;

    @NotNull
    @JsonProperty("isPublic")
    private boolean publicChatbot;
}
