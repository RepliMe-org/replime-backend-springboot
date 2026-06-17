package com.example.demo.services.rabbitMQService;

import com.example.demo.dtos.internal.VideoIndexMessage;
import com.example.demo.entities.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class VideoIndexPublisher {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${replime.ingestion.rabbitmq.exchange}")
    private String exchange;

    @Value("${replime.ingestion.rabbitmq.routing-key}")
    private String routingKey;

    /**
     * Publishes one RabbitMQ message per video.
     * WHY one message per video (not one message for the whole list)?
     */
    public void publishForIndexing(List<Video> videos) {
        for (Video video : videos) {
            publishSingle(video, 1, "TRANSCRIPT_EXTRACTION");
        }
    }

    /**
     * Called by RetryScheduler and the manual retry endpoint.
     * Publishes a single video back into the queue.
     */
    public void publishForRetry(Video video) {
        int nextAttempt = video.getRetryCount() + 1;

        // Start from the stage that failed — don't redo completed work
        String startFromStage = video.getFailedStage() != null
                ? video.getFailedStage().name()
                : "TRANSCRIPT_EXTRACTION";

        publishSingle(video, nextAttempt, startFromStage);
        log.info("Re-published video {} for retry (attempt {}/{})",
                video.getId(), nextAttempt, video.getMaxRetries());
    }

    private void publishSingle(Video video,
                               int attemptNumber,
                               String startFromStage) {
        String idempotencyKey = "video:" + video.getId()
                + ":attempt:" + attemptNumber;

        VideoIndexMessage message = VideoIndexMessage.builder()
                .youtubeVideoId(video.getYoutubeVideoId())
                .video_title(video.getTitle())
                .chatbotId(video.getTrainingSource().getChatbot().getId().toString())
                .trainingSourceId(video.getTrainingSource().getId())
                .idempotencyKey(idempotencyKey)
                .attemptNumber(attemptNumber)
                .startFromStage(startFromStage)
                .publishedAt(OffsetDateTime.now().toString())
                .build();

        rabbitTemplate.convertAndSend(exchange, routingKey, message,
                msg -> {
                    // Correlation ID lets you match this message to a job
                    // in RabbitMQ management UI
                    msg.getMessageProperties()
                            .setCorrelationId(video.getId().toString());
                    msg.getMessageProperties()
                            .setMessageId(idempotencyKey);
                    return msg;
                });

        log.info("Published video {} to RabbitMQ (attempt {}, startFrom={})",
                video.getId(), attemptNumber, startFromStage);
    }
}
