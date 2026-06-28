package com.example.demo.controllers;

import com.example.demo.dtos.internal.UpdateVideoStatusRequestDTO;
import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.services.MessageService;
import com.example.demo.services.VideoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for internal video/message endpoints and websocket test hook,
// verifying service delegation, topic payload forwarding, and fixed success response.
class InternalControllerTest {

    @Test
    void updateVideoStatusPassesVideoIdAndRequest() throws Exception {
        InternalController controller = new InternalController();
        TestVideoService videoService = new TestVideoService();
        injectField(controller, "videoService", videoService);
        UpdateVideoStatusRequestDTO request = new UpdateVideoStatusRequestDTO();
        request.setFailureReason("failed");

        ResponseEntity<String> response = controller.updateVideoStatus("yt-123", request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Video status updated successfully", response.getBody());
        assertEquals("yt-123", videoService.videoId.get());
        assertSame(request, videoService.updateRequest.get());
    }

    @Test
    void testWsSendsHelloToTestTopic() throws Exception {
        InternalController controller = new InternalController();
        TestMessagingTemplate messagingTemplate = new TestMessagingTemplate();
        injectField(controller, "messagingTemplate", messagingTemplate);

        controller.testWs();

        assertEquals("/topic/test", messagingTemplate.destination.get());
        assertEquals("HELLO", messagingTemplate.payload.get());
    }

    @Test
    void updateMessageClassesPassesIdsAndReturnsMessage() throws Exception {
        InternalController controller = new InternalController();
        TestMessageService messageService = new TestMessageService();
        MessageDto serviceResponse = MessageDto.builder().id(11L).messageClass("Pricing").build();
        messageService.response = serviceResponse;
        injectField(controller, "messageService", messageService);

        ResponseEntity<MessageDto> response = controller.updateMessageClasses(11L, 3L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertEquals(11L, messageService.messageId.get());
        assertEquals(3L, messageService.messageClassId.get());
    }

    private static void injectField(InternalController controller, String name, Object value) throws Exception {
        Field field = InternalController.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(controller, value);
    }

    private static class TestVideoService extends VideoService {
        private final AtomicReference<String> videoId = new AtomicReference<>();
        private final AtomicReference<UpdateVideoStatusRequestDTO> updateRequest = new AtomicReference<>();

        @Override
        public void updateVideoStatus(String youtubeVideoId, UpdateVideoStatusRequestDTO request) {
            videoId.set(youtubeVideoId);
            updateRequest.set(request);
        }
    }

    private static class TestMessageService extends MessageService {
        private MessageDto response;
        private final AtomicReference<Long> messageId = new AtomicReference<>();
        private final AtomicReference<Long> messageClassId = new AtomicReference<>();

        @Override
        public MessageDto classifyMessage(Long messageId, Long messageClassId) {
            this.messageId.set(messageId);
            this.messageClassId.set(messageClassId);
            return response;
        }
    }

    private static class TestMessagingTemplate extends SimpMessagingTemplate {
        private final AtomicReference<String> destination = new AtomicReference<>();
        private final AtomicReference<Object> payload = new AtomicReference<>();

        private TestMessagingTemplate() {
            super(new NoopMessageChannel());
        }

        @Override
        public void convertAndSend(String destination, Object payload) {
            this.destination.set(destination);
            this.payload.set(payload);
        }
    }

    private static class NoopMessageChannel implements MessageChannel {
        @Override
        public boolean send(org.springframework.messaging.Message<?> message) {
            return true;
        }

        @Override
        public boolean send(org.springframework.messaging.Message<?> message, long timeout) {
            return true;
        }
    }
}
