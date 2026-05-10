package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.SessionListResponseDTO;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.User;
import com.example.demo.exceptions.AuthenticationException;
import com.example.demo.exceptions.InvalidSourceException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.ChatbotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepo chatSessionRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatbotRepo chatbotRepo;

    public SessionResponseDTO createSession(UUID chatbotId, String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findById(chatbotId).orElseThrow(() -> new RuntimeException("Chatbot not found"));
        ChatSession chatSession = ChatSession.builder()
                .chatbot(chatbot)
                .user(user)
                .build();
        chatSessionRepo.save(chatSession);
        return mapToSessionResponseDTO(chatSession);
    }
    private SessionResponseDTO mapToSessionResponseDTO(ChatSession chatSession) {
        return SessionResponseDTO.builder()
                .sessionId(chatSession.getId())
                .chatbotId(chatSession.getChatbot().getId())
                .chatbotName(chatSession.getChatbot().getConfig().getName())
                .greetingMessage(chatSession.getChatbot().getConfig().getGreetingMessage())
                .startedAt(chatSession.getStartedAt())
                .messageCount(chatSession.getMessages().size())
                .build();
    }

    public SessionResponseDTO getSessionDetails(Long sessionId, String token) {
        User  user = jwtService.extractUser(token);
        ChatSession chatSession;
        try {
            chatSession = chatSessionRepo.findById(sessionId).get();
        } catch (NoSuchElementException e) {
            throw new ResourceNotFoundException("Chat session not found");
        }
        if (!chatSession.getUser().getId().equals(user.getId())) {
            throw new AuthenticationException("Unauthorized access to chat session");
        }
        return mapToSessionResponseDTO(chatSession);
    }

    public SessionListResponseDTO getAllSessions(
            String token, UUID chatbotId, String cursor, Integer limit) {

        User user = jwtService.extractUser(token);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Pageable pageable = PageRequest.of(0, safeLimit + 1);

        LocalDateTime cursorTime;
        Long cursorId;

        List<ChatSession> sessions;

        if (cursor != null && !cursor.isBlank()) {
            try{
                CursorData data = decodeCursor(cursor);
                cursorTime = data.time();
                cursorId = data.id();
                sessions = chatSessionRepo.findSessions(
                        user.getId(), chatbotId, cursorTime, cursorId, safeLimit + 1
                );
            } catch (InvalidSourceException e) {
                throw new InvalidSourceException("Invalid pagination cursor");
            }
        }else {
            sessions = chatSessionRepo.findFirstPage(
                    user.getId(), chatbotId, pageable
            );
        }

        return mapToSessionListResponseDTO(sessions, safeLimit);
    }

    private SessionListResponseDTO mapToSessionListResponseDTO(
            List<ChatSession> sessions, int limit) {

        boolean hasMore = sessions.size() > limit;

        List<ChatSession> page = hasMore
                ? sessions.subList(0, limit)
                : sessions;

        List<SessionListResponseDTO.SessionItem> sessionItems = new ArrayList<>();
        for (ChatSession chatSession : page) {
            SessionListResponseDTO.SessionItem sessionItem =
                    new SessionListResponseDTO.SessionItem();
            sessionItem.setId(chatSession.getId());
            sessionItem.setStatus(chatSession.getStatus());
            sessionItem.setStartedAt(chatSession.getStartedAt());
            sessionItem.setLastMessageAt(chatSession.getLastMessageAt());
            sessionItem.setSessionTopic(chatSession.getSessionTopic());
            sessionItems.add(sessionItem);
        }

        String nextCursor = null;
        if (hasMore) {
            ChatSession last = page.get(page.size() - 1);
            nextCursor = encodeCursor(last.getLastMessageAt(), last.getId());
        }

        SessionListResponseDTO.PaginationInfo paginationInfo =
                SessionListResponseDTO.PaginationInfo.builder()
                        .hasMore(hasMore)
                        .nextCursor(nextCursor)
                        .limit(limit)
                        .build();

        return SessionListResponseDTO.builder()
                .data(sessionItems)
                .pagination(paginationInfo)
                .build();
    }

    private String encodeCursor(LocalDateTime lastMessageAt, Long id) {
        try {
            // Format: "2025-05-04T10:31:00|42"  then Base64-encode
            String raw = lastMessageAt.toString() + "|" + id.toString();
            return Base64.getUrlEncoder()
                    .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    private record CursorData(LocalDateTime time, Long id) {}

    private CursorData decodeCursor(String cursor) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = raw.split("\\|");
            return new CursorData(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
        } catch (Exception e) {
            throw new InvalidSourceException("Invalid pagination cursor");
        }
    }
}
