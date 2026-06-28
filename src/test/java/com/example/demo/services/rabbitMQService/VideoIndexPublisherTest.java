package com.example.demo.services.rabbitMQService;

import com.example.demo.dtos.internal.VideoIndexMessage;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.TrainingSource;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.FailedStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

// Coverage criteria: Rabbit publisher unit coverage for initial indexing and retry publishing,
// verifying one message per video, exchange/routing key, payload fields, attempt/idempotency values, retry start stage, and AMQP message metadata.
class VideoIndexPublisherTest {

    private static final String EXCHANGE = "video.exchange";
    private static final String ROUTING_KEY = "video.index";

    private RabbitTemplate rabbitTemplate;
    private VideoIndexPublisher publisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        publisher = new VideoIndexPublisher();
        ReflectionTestUtils.setField(publisher, "rabbitTemplate", rabbitTemplate);
        ReflectionTestUtils.setField(publisher, "exchange", EXCHANGE);
        ReflectionTestUtils.setField(publisher, "routingKey", ROUTING_KEY);
    }

    @Test
    void publishForIndexingPublishesOneInitialMessagePerVideo() {
        Video first = video(10L, "yt-10", "First video", 0, null);
        Video second = video(20L, "yt-20", "Second video", 0, null);
        ArgumentCaptor<VideoIndexMessage> messageCaptor = ArgumentCaptor.forClass(VideoIndexMessage.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        publisher.publishForIndexing(List.of(first, second));

        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(EXCHANGE),
                eq(ROUTING_KEY),
                messageCaptor.capture(),
                processorCaptor.capture());
        assertInitialMessage(messageCaptor.getAllValues().get(0), first);
        assertInitialMessage(messageCaptor.getAllValues().get(1), second);
        assertMessageProperties(processorCaptor.getAllValues().get(0), first.getId(), "video:10:attempt:1");
        assertMessageProperties(processorCaptor.getAllValues().get(1), second.getId(), "video:20:attempt:1");
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publishForRetryUsesNextAttemptAndFailedStage() {
        Video video = video(30L, "yt-30", "Retry me", 2, FailedStage.VECTOR_INDEXING);
        ArgumentCaptor<VideoIndexMessage> messageCaptor = ArgumentCaptor.forClass(VideoIndexMessage.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);

        publisher.publishForRetry(video);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), messageCaptor.capture(), processorCaptor.capture());
        VideoIndexMessage message = messageCaptor.getValue();
        assertCommonMessageFields(message, video);
        assertEquals(3, message.getAttemptNumber());
        assertEquals("video:30:attempt:3", message.getIdempotencyKey());
        assertEquals("VECTOR_INDEXING", message.getStartFromStage());
        assertMessageProperties(processorCaptor.getValue(), video.getId(), "video:30:attempt:3");
    }

    @Test
    void publishForRetryFallsBackToTranscriptExtractionWhenFailedStageIsMissing() {
        Video video = video(40L, "yt-40", "Retry from start", 1, null);
        ArgumentCaptor<VideoIndexMessage> messageCaptor = ArgumentCaptor.forClass(VideoIndexMessage.class);

        publisher.publishForRetry(video);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), messageCaptor.capture(), org.mockito.ArgumentMatchers.any(MessagePostProcessor.class));
        assertEquals(2, messageCaptor.getValue().getAttemptNumber());
        assertEquals("TRANSCRIPT_EXTRACTION", messageCaptor.getValue().getStartFromStage());
        assertEquals("video:40:attempt:2", messageCaptor.getValue().getIdempotencyKey());
    }

    private static void assertInitialMessage(VideoIndexMessage message, Video video) {
        assertCommonMessageFields(message, video);
        assertEquals(1, message.getAttemptNumber());
        assertEquals("video:" + video.getId() + ":attempt:1", message.getIdempotencyKey());
        assertEquals("TRANSCRIPT_EXTRACTION", message.getStartFromStage());
    }

    private static void assertCommonMessageFields(VideoIndexMessage message, Video video) {
        assertEquals(video.getYoutubeVideoId(), message.getYoutubeVideoId());
        assertEquals(video.getTitle(), message.getVideoTitle());
        assertEquals(video.getTrainingSource().getChatbot().getId().toString(), message.getChatbotId());
        assertEquals(video.getTrainingSource().getId(), message.getTrainingSourceId());
        assertNotNull(OffsetDateTime.parse(message.getPublishedAt()));
    }

    private static void assertMessageProperties(
            MessagePostProcessor processor,
            Long videoId,
            String idempotencyKey
    ) {
        Message message = processor.postProcessMessage(new Message(new byte[0], new MessageProperties()));

        assertEquals(videoId.toString(), message.getMessageProperties().getCorrelationId());
        assertEquals(idempotencyKey, message.getMessageProperties().getMessageId());
    }

    private static Video video(
            Long videoId,
            String youtubeVideoId,
            String title,
            int retryCount,
            FailedStage failedStage
    ) {
        Chatbot chatbot = new Chatbot();
        chatbot.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        TrainingSource trainingSource = new TrainingSource();
        trainingSource.setId(99L);
        trainingSource.setChatbot(chatbot);

        Video video = new Video();
        video.setId(videoId);
        video.setYoutubeVideoId(youtubeVideoId);
        video.setTitle(title);
        video.setRetryCount(retryCount);
        video.setMaxRetries(3);
        video.setFailedStage(failedStage);
        video.setTrainingSource(trainingSource);
        return video;
    }
}
