package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.User;
import com.example.demo.exceptions.AuthenticationException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.ChatbotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

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
}
