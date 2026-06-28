package com.example.demo.controllers;

import com.example.demo.dtos.AdminChatbotResponseDTO;
import com.example.demo.services.ChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for all admin chatbot endpoints,
// verifying service delegation, path/query argument forwarding, and response passthrough.
class AdminChatbotControllerTest {

    @Test
    void getAllChatbotsReturnsServiceResponse() throws Exception {
        AdminChatbotController controller = new AdminChatbotController();
        ResponseEntity<List<AdminChatbotResponseDTO>> serviceResponse = ResponseEntity.ok(List.of(
                AdminChatbotResponseDTO.builder().chatbotName("Bot").build()));
        TestChatbotService service = new TestChatbotService();
        service.allChatbotsResponse = serviceResponse;
        injectService(controller, service);

        ResponseEntity<List<AdminChatbotResponseDTO>> response = controller.getAllChatbots();

        assertSame(serviceResponse, response);
    }

    @Test
    void updateChatbotVisibilityPassesIdAndVisibilityToService() throws Exception {
        AdminChatbotController controller = new AdminChatbotController();
        ResponseEntity<String> serviceResponse = ResponseEntity.ok("updated");
        TestChatbotService service = new TestChatbotService();
        service.visibilityResponse = serviceResponse;
        injectService(controller, service);
        UUID chatbotId = UUID.randomUUID();

        ResponseEntity<String> response = controller.updateChatbotVisibility(chatbotId, true);

        assertSame(serviceResponse, response);
        assertEquals(chatbotId, service.visibilityChatbotId.get());
        assertEquals(true, service.visibilityValue.get());
    }

    @Test
    void deleteChatbotPassesIdToService() throws Exception {
        AdminChatbotController controller = new AdminChatbotController();
        ResponseEntity<String> serviceResponse = ResponseEntity.ok("deleted");
        TestChatbotService service = new TestChatbotService();
        service.deleteResponse = serviceResponse;
        injectService(controller, service);
        UUID chatbotId = UUID.randomUUID();

        ResponseEntity<String> response = controller.deleteChatbot(chatbotId);

        assertSame(serviceResponse, response);
        assertEquals(chatbotId, service.deleteChatbotId.get());
    }

    private static void injectService(AdminChatbotController controller, ChatbotService service) throws Exception {
        Field field = AdminChatbotController.class.getDeclaredField("chatbotService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestChatbotService extends ChatbotService {
        private ResponseEntity<List<AdminChatbotResponseDTO>> allChatbotsResponse;
        private ResponseEntity<String> visibilityResponse;
        private ResponseEntity<String> deleteResponse;
        private final AtomicReference<UUID> visibilityChatbotId = new AtomicReference<>();
        private final AtomicReference<Boolean> visibilityValue = new AtomicReference<>();
        private final AtomicReference<UUID> deleteChatbotId = new AtomicReference<>();

        @Override
        public ResponseEntity<List<AdminChatbotResponseDTO>> getAllChatbotsForAdmin() {
            return allChatbotsResponse;
        }

        @Override
        public ResponseEntity<String> updateChatbotVisibility(UUID id, boolean isPublic) {
            visibilityChatbotId.set(id);
            visibilityValue.set(isPublic);
            return visibilityResponse;
        }

        @Override
        public ResponseEntity<String> deleteChatbotForAdmin(UUID id) {
            deleteChatbotId.set(id);
            return deleteResponse;
        }
    }
}
