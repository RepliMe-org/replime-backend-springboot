package com.example.demo.controllers;

import com.example.demo.dtos.ApiResponseDTO;
import com.example.demo.dtos.ChatSessionSearchResponseDTO;
import com.example.demo.dtos.CreateSessionRequestDTO;
import com.example.demo.dtos.SendMessageResponseDTO;
import com.example.demo.dtos.SessionListResponseDTO;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.services.ChatSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Coverage criteria: direct controller unit coverage for all chat-session endpoints,
// verifying request/path/query/header forwarding plus generated delete success DTO.
class ChatSessionControllerTest {

    @Test
    void createSessionPassesChatbotIdAndToken() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        service.sessionResponse = SessionResponseDTO.builder().sessionId(1L).build();
        injectService(controller, service);
        UUID chatbotId = UUID.randomUUID();
        CreateSessionRequestDTO request = CreateSessionRequestDTO.builder().chatbotId(chatbotId).build();

        ResponseEntity<SessionResponseDTO> response = controller.createSession(request, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.sessionResponse, response.getBody());
        assertEquals(chatbotId, service.createdChatbotId.get());
        assertEquals("Bearer token", service.createToken.get());
    }

    @Test
    void getSessionDetailsPassesSessionIdAndToken() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        service.sessionResponse = SessionResponseDTO.builder().sessionId(9L).build();
        injectService(controller, service);

        ResponseEntity<SessionResponseDTO> response = controller.getSessionDetails(9L, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.sessionResponse, response.getBody());
        assertEquals(9L, service.detailsSessionId.get());
        assertEquals("Bearer token", service.detailsToken.get());
    }

    @Test
    void getAllSessionsPassesPaginationArguments() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        service.sessionListResponse = SessionListResponseDTO.builder().build();
        injectService(controller, service);
        UUID chatbotId = UUID.randomUUID();

        ResponseEntity<SessionListResponseDTO> response =
                controller.getAllSessions("Bearer token", chatbotId, "cursor", 15);

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.sessionListResponse, response.getBody());
        assertEquals("Bearer token", service.listToken.get());
        assertEquals(chatbotId, service.listChatbotId.get());
        assertEquals("cursor", service.listCursor.get());
        assertEquals(15, service.listLimit.get());
    }

    @Test
    void searchSessionsPassesSearchArguments() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        service.searchResponse = ChatSessionSearchResponseDTO.builder().query("pricing").build();
        injectService(controller, service);
        UUID chatbotId = UUID.randomUUID();

        ResponseEntity<ChatSessionSearchResponseDTO> response =
                controller.searchSessions("Bearer token", chatbotId, "pricing");

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.searchResponse, response.getBody());
        assertEquals("Bearer token", service.searchToken.get());
        assertEquals(chatbotId, service.searchChatbotId.get());
        assertEquals("pricing", service.searchQuery.get());
    }

    @Test
    void sendMessagePassesMessageArguments() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        service.sendMessageResponse = SendMessageResponseDTO.builder().sessionId(5L).build();
        injectService(controller, service);

        ResponseEntity<SendMessageResponseDTO> response =
                controller.sendMessage(5L, "hello", "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.sendMessageResponse, response.getBody());
        assertEquals(5L, service.messageSessionId.get());
        assertEquals("hello", service.userMessage.get());
        assertEquals("Bearer token", service.messageToken.get());
    }

    @Test
    void getSessionMessagesReturnsServiceMessages() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        service.messages = List.of(MessageDto.builder().id(1L).message("hello").build());
        injectService(controller, service);

        ResponseEntity<List<MessageDto>> response = controller.getSessionMessages(5L, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.messages, response.getBody());
        assertEquals(5L, service.messagesSessionId.get());
        assertEquals("Bearer token", service.messagesToken.get());
    }

    @Test
    void deleteSessionPassesArgumentsAndReturnsSuccessDto() throws Exception {
        ChatSessionController controller = new ChatSessionController();
        TestChatSessionService service = new TestChatSessionService();
        injectService(controller, service);

        ResponseEntity<ApiResponseDTO> response = controller.deleteSession(5L, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Session deleted successfully", response.getBody().getMessage());
        assertEquals("Bearer token", service.deleteToken.get());
        assertEquals(5L, service.deleteSessionId.get());
    }

    private static void injectService(ChatSessionController controller, ChatSessionService service) throws Exception {
        Field field = ChatSessionController.class.getDeclaredField("chatSessionService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestChatSessionService extends ChatSessionService {
        private SessionResponseDTO sessionResponse;
        private SessionListResponseDTO sessionListResponse;
        private ChatSessionSearchResponseDTO searchResponse;
        private SendMessageResponseDTO sendMessageResponse;
        private List<MessageDto> messages;
        private final AtomicReference<UUID> createdChatbotId = new AtomicReference<>();
        private final AtomicReference<String> createToken = new AtomicReference<>();
        private final AtomicReference<Long> detailsSessionId = new AtomicReference<>();
        private final AtomicReference<String> detailsToken = new AtomicReference<>();
        private final AtomicReference<String> listToken = new AtomicReference<>();
        private final AtomicReference<UUID> listChatbotId = new AtomicReference<>();
        private final AtomicReference<String> listCursor = new AtomicReference<>();
        private final AtomicReference<Integer> listLimit = new AtomicReference<>();
        private final AtomicReference<String> searchToken = new AtomicReference<>();
        private final AtomicReference<UUID> searchChatbotId = new AtomicReference<>();
        private final AtomicReference<String> searchQuery = new AtomicReference<>();
        private final AtomicReference<Long> messageSessionId = new AtomicReference<>();
        private final AtomicReference<String> userMessage = new AtomicReference<>();
        private final AtomicReference<String> messageToken = new AtomicReference<>();
        private final AtomicReference<Long> messagesSessionId = new AtomicReference<>();
        private final AtomicReference<String> messagesToken = new AtomicReference<>();
        private final AtomicReference<String> deleteToken = new AtomicReference<>();
        private final AtomicReference<Long> deleteSessionId = new AtomicReference<>();

        @Override
        public SessionResponseDTO createSession(UUID chatbotId, String token) {
            createdChatbotId.set(chatbotId);
            createToken.set(token);
            return sessionResponse;
        }

        @Override
        public SessionResponseDTO getSessionDetails(Long sessionId, String token) {
            detailsSessionId.set(sessionId);
            detailsToken.set(token);
            return sessionResponse;
        }

        @Override
        public SessionListResponseDTO getAllSessions(String token, UUID chatbotId, String cursor, Integer limit) {
            listToken.set(token);
            listChatbotId.set(chatbotId);
            listCursor.set(cursor);
            listLimit.set(limit);
            return sessionListResponse;
        }

        @Override
        public ChatSessionSearchResponseDTO searchSessions(String token, UUID chatbotId, String query) {
            searchToken.set(token);
            searchChatbotId.set(chatbotId);
            searchQuery.set(query);
            return searchResponse;
        }

        @Override
        public SendMessageResponseDTO sendMessage(Long sessionId, String userMessage, String token) {
            messageSessionId.set(sessionId);
            this.userMessage.set(userMessage);
            messageToken.set(token);
            return sendMessageResponse;
        }

        @Override
        public List<MessageDto> getSessionMessages(Long sessionId, String token) {
            messagesSessionId.set(sessionId);
            messagesToken.set(token);
            return messages;
        }

        @Override
        public void deleteSession(String token, Long sessionId) {
            deleteToken.set(token);
            deleteSessionId.set(sessionId);
        }
    }
}
