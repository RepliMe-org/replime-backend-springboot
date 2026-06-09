package com.example.demo.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
public class SessionResponseDTO {
    Long sessionId;
    UUID chatbotId;
    String chatbotName;
    String greetingMessage;
    LocalDateTime startedAt;
    Integer messageCount;
    String sessionTopic;
}
