package com.example.demo.controllers;

import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for public chatbot listing/detail endpoints,
// verifying service response passthrough and chatbot id forwarding.
class PublicChatbotControllerTest {

    @Test
    void getAllPublicChatbotsReturnsServiceResponse() throws Exception {
        PublicChatbotController controller = new PublicChatbotController();
        ResponseEntity<List<PublicChatbotResponseDTO>> serviceResponse = ResponseEntity.ok(List.of(
                PublicChatbotResponseDTO.builder().chatbotName("Bot").build()));
        TestChatbotService service = new TestChatbotService();
        service.publicChatbotsResponse = serviceResponse;
        injectService(controller, service);

        ResponseEntity<List<PublicChatbotResponseDTO>> response = controller.getAllPublicChatbots();

        assertSame(serviceResponse, response);
    }

    @Test
    void getChatbotByIdPassesIdToService() throws Exception {
        PublicChatbotController controller = new PublicChatbotController();
        ResponseEntity<PublicChatbotResponseDTO> serviceResponse = ResponseEntity.ok(
                PublicChatbotResponseDTO.builder().chatbotName("Bot").build());
        TestChatbotService service = new TestChatbotService();
        service.chatbotByIdResponse = serviceResponse;
        injectService(controller, service);
        UUID chatbotId = UUID.randomUUID();

        ResponseEntity<PublicChatbotResponseDTO> response = controller.getChatbotById(chatbotId);

        assertSame(serviceResponse, response);
        assertEquals(chatbotId, service.chatbotId.get());
    }

    private static void injectService(PublicChatbotController controller, ChatbotService service) throws Exception {
        Field field = PublicChatbotController.class.getDeclaredField("chatbotService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestChatbotService extends ChatbotService {
        private ResponseEntity<List<PublicChatbotResponseDTO>> publicChatbotsResponse;
        private ResponseEntity<PublicChatbotResponseDTO> chatbotByIdResponse;
        private final AtomicReference<UUID> chatbotId = new AtomicReference<>();

        @Override
        public ResponseEntity<List<PublicChatbotResponseDTO>> getPublicChatbots() {
            return publicChatbotsResponse;
        }

        @Override
        public ResponseEntity<PublicChatbotResponseDTO> getChatbotById(UUID id) {
            chatbotId.set(id);
            return chatbotByIdResponse;
        }
    }
}
