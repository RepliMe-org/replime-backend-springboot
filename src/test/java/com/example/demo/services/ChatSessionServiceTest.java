package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ChatSessionSearchResponseDTO;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.Message;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.exceptions.AuthenticationException;
import com.example.demo.exceptions.InvalidSourceException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageRepo;
import com.example.demo.repos.VideoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for chat-session creation/access/list/search/delete behavior,
// verifying ownership checks, missing sessions, pagination limit forwarding, search validation, DTO mapping, and soft delete.
class ChatSessionServiceTest {

    @Test
    void createSessionSavesSessionAndReturnsMappedDto() {
        ChatSessionRepo sessionRepo = mock(ChatSessionRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatSessionService service = service(sessionRepo, chatbotRepo, jwtService, mock(MessageRepo.class));
        User user = User.builder().id(1L).email("user@example.com").build();
        Chatbot chatbot = chatbot();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findById(chatbot.getId())).thenReturn(Optional.of(chatbot));
        when(sessionRepo.save(org.mockito.ArgumentMatchers.any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession session = invocation.getArgument(0);
                    session.setId(9L);
                    session.setStartedAt(LocalDateTime.of(2026, 1, 1, 10, 0));
                    return session;
                });

        SessionResponseDTO response = service.createSession(chatbot.getId(), "Bearer token");

        assertEquals(9L, response.getSessionId());
        assertEquals(chatbot.getId(), response.getChatbotId());
        assertEquals("Creator Bot", response.getChatbotName());
        assertEquals("Hello", response.getGreetingMessage());
        assertEquals(0, response.getMessageCount());
    }

    @Test
    void getSessionDetailsThrowsWhenSessionMissing() {
        ChatSessionRepo sessionRepo = mock(ChatSessionRepo.class);
        ChatSessionService service = service(sessionRepo, mock(ChatbotRepo.class), mock(JwtService.class), mock(MessageRepo.class));
        when(sessionRepo.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getSessionDetails(99L, "Bearer token"));

        assertEquals("Chat session not found", exception.getMessage());
    }

    @Test
    void getSessionDetailsThrowsWhenUserDoesNotOwnSession() {
        ChatSessionRepo sessionRepo = mock(ChatSessionRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatSessionService service = service(sessionRepo, mock(ChatbotRepo.class), jwtService, mock(MessageRepo.class));
        ChatSession session = ChatSession.builder()
                .id(9L)
                .user(User.builder().id(2L).build())
                .chatbot(chatbot())
                .build();
        when(jwtService.extractUser("Bearer token")).thenReturn(User.builder().id(1L).build());
        when(sessionRepo.findById(9L)).thenReturn(Optional.of(session));

        AuthenticationException exception = assertThrows(
                AuthenticationException.class,
                () -> service.getSessionDetails(9L, "Bearer token"));

        assertEquals("Unauthorized access to chat session", exception.getMessage());
    }

    @Test
    void getAllSessionsClampsLimitAndReturnsNextCursorWhenMoreRowsExist() {
        ChatSessionRepo sessionRepo = mock(ChatSessionRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatSessionService service = service(sessionRepo, mock(ChatbotRepo.class), jwtService, mock(MessageRepo.class));
        User user = User.builder().id(1L).build();
        UUID chatbotId = UUID.randomUUID();
        ChatSession first = ChatSession.builder()
                .id(3L)
                .lastMessageAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .status(ChatSessionStatus.ACTIVE)
                .build();
        ChatSession extra = ChatSession.builder()
                .id(2L)
                .lastMessageAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .status(ChatSessionStatus.ACTIVE)
                .build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(sessionRepo.findFirstPage(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(chatbotId),
                org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(List.of(first, extra));

        var response = service.getAllSessions("Bearer token", chatbotId, null, 1);

        assertEquals(1, response.getData().size());
        assertEquals(1, response.getPagination().getLimit());
        assertEquals(true, response.getPagination().isHasMore());
        assertNotNull(response.getPagination().getNextCursor());
    }

    @Test
    void searchSessionsRejectsBlankQuery() {
        ChatSessionService service = service(mock(ChatSessionRepo.class), mock(ChatbotRepo.class), mock(JwtService.class), mock(MessageRepo.class));

        InvalidSourceException exception = assertThrows(
                InvalidSourceException.class,
                () -> service.searchSessions("Bearer token", UUID.randomUUID(), " "));

        assertEquals("Search query cannot be empty", exception.getMessage());
    }

    @Test
    void searchSessionsMapsMatches() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatSessionService service = service(mock(ChatSessionRepo.class), mock(ChatbotRepo.class), jwtService, messageRepo);
        User user = User.builder().id(1L).build();
        UUID chatbotId = UUID.randomUUID();
        Chatbot chatbot = Chatbot.builder().id(chatbotId).build();
        ChatSession session = ChatSession.builder().id(7L).chatbot(chatbot).sessionTopic("Topic").build();
        Message message = Message.builder()
                .id(11L)
                .session(session)
                .content("pricing question")
                .sender(MessageSender.USER)
                .sentAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(messageRepo.searchUserChatbotMessages(1L, chatbotId, "pricing")).thenReturn(List.of(message));

        ChatSessionSearchResponseDTO response = service.searchSessions("Bearer token", chatbotId, " pricing ");

        assertEquals("pricing", response.getQuery());
        assertEquals(1, response.getMatchCount());
        assertEquals(7L, response.getData().get(0).getSessionId());
        assertEquals(11L, response.getData().get(0).getMessageId());
    }

    @Test
    void deleteSessionMarksSessionDeleted() {
        ChatSessionRepo sessionRepo = mock(ChatSessionRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatSessionService service = service(sessionRepo, mock(ChatbotRepo.class), jwtService, mock(MessageRepo.class));
        User user = User.builder().id(1L).build();
        ChatSession session = ChatSession.builder().id(7L).user(user).status(ChatSessionStatus.ACTIVE).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(sessionRepo.findById(7L)).thenReturn(Optional.of(session));

        service.deleteSession("Bearer token", 7L);

        assertEquals(ChatSessionStatus.DELETED, session.getStatus());
        verify(sessionRepo).save(session);
    }

    private static Chatbot chatbot() {
        return Chatbot.builder()
                .id(UUID.randomUUID())
                .config(ChatbotConfig.builder()
                        .name("Creator Bot")
                        .greetingMessage("Hello")
                        .build())
                .build();
    }

    private static ChatSessionService service(
            ChatSessionRepo sessionRepo,
            ChatbotRepo chatbotRepo,
            JwtService jwtService,
            MessageRepo messageRepo
    ) {
        ChatSessionService service = new ChatSessionService();
        ReflectionTestUtils.setField(service, "chatSessionRepo", sessionRepo);
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        ReflectionTestUtils.setField(service, "messageRepo", messageRepo);
        ReflectionTestUtils.setField(service, "messageService", mock(MessageService.class));
        ReflectionTestUtils.setField(service, "fastApiService", mock(FastApiService.class));
        ReflectionTestUtils.setField(service, "videoService", mock(VideoService.class));
        ReflectionTestUtils.setField(service, "videoRepository", mock(VideoRepository.class));
        ReflectionTestUtils.setField(service, "messageClassService", mock(MessageClassService.class));
        return service;
    }
}
