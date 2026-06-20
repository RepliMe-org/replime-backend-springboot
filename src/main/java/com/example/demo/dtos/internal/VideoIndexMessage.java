package com.example.demo.dtos.internal;

// com/replime/dto/VideoIndexMessage.java
// This is the JSON message that travels through RabbitMQ.
// FastAPI's worker deserializes this.

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoIndexMessage {

    private String youtubeVideoId;

    private String chatbotId;

    private String videoTitle;

    private String description;

    private Long trainingSourceId;

    /**
     * Idempotency key: prevents processing the same video twice.
     * Format: "video:{videoId}:attempt:{attemptNumber}"
     *
     * If FastAPI's worker crashes after processing but before
     * the webhook is received, Spring Boot may re-publish.
     * The worker checks this key in Redis before processing.
     * If already processed → send COMPLETED webhook and ACK.
     * No duplicate vectors ever.
     */
    private String idempotencyKey;

    private Integer attemptNumber;

    private String startFromStage;

    private String publishedAt;
}
