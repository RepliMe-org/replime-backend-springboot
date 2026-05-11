package com.example.demo.dtos;


import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.dtos.internal.BotQueryResponseDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
public class SendMessageResponseDTO {
    private Long sessionId;

    private String sessionTitle;

    private MessageDto userMessage;

    private MessageDto aiResponse;

    private List<Source> sources;

    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String videoId;
        private String videoTitle;
        private String youtubeUrl;
        private String thumbnailUrl;
    }
}
