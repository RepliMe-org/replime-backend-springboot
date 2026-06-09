package com.example.demo.dtos;

import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.Formality;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
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

    private ChatbotInfo chatbotInfo;
    private ConfigInfo configInfo;

    @Getter
    @Setter
    @Builder
    public static class ChatbotInfo {
        private UUID id;
        private String influencerUsername;
        private ChatbotStatus status;
        private boolean isPublic;
        private LocalDateTime createdAt;
    }

    @Getter
    @Setter
    @Builder
    public static class ConfigInfo {
        private Long configId;
        private String chatbotName;
        private String chatbotDescription;
        private String greetingMessage;
        private Integer avatarNumber;
        private boolean talkLikeMe;
        private Tone tone;
        private Verbosity verbosity;
        private Formality formality;
        private LocalDateTime configCreatedAt;
    }
}
