package com.example.demo.dtos.internal;

import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.utils.MessageSender;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotQueryRequestDTO {

    @JsonProperty("chatbot_id")
    private String chatbotId;

    @JsonProperty("message_id")
    @Nullable
    private Long messageId;

    private String query;

    @JsonProperty("conversation_history")
    private List<ConversationHistoryDTO> conversationHistory;

    private ConfigDTO config;

    @JsonProperty("message_classes")
    private List<MessageClassResponseDTO> messageClasses;

    @JsonProperty("first_message")
    private Boolean firstMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationHistoryDTO {
        private String role;
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfigDTO {

        @JsonProperty("chatbot_name")
        private String chatbotName;

        @JsonProperty("talk_like_me")
        private Boolean talkLikeMe;

        @Nullable
        private String tone;

        private String verbosity;

        @Nullable
        private String formality;
    }
}