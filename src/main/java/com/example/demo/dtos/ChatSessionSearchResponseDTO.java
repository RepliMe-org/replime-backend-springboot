package com.example.demo.dtos;

import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.utils.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionSearchResponseDTO {
    private String query;
    private Integer matchCount;
    private List<SearchMatch> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMatch {
        private Long sessionId;
        private String sessionTitle;
        private UUID chatbotId;
        private Long messageId;
        private String matchedMessage;
        private MessageSender sender;
        private LocalDateTime sentAt;
    }
}
