package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.dtos.SendMessageResponseDTO;
import com.example.demo.dtos.SessionListResponseDTO;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.dtos.internal.BotQueryRequestDTO;
import com.example.demo.dtos.internal.BotQueryResponseDTO;
import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Message;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.MessageClass;
import com.example.demo.exceptions.AuthenticationException;
import com.example.demo.exceptions.InvalidSourceException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.ChatbotRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


import org.springframework.data.domain.Pageable;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import com.example.demo.dtos.utils.CursorData;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepo chatSessionRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatbotRepo chatbotRepo;
    @Autowired
    private MessageService messageService;
    @Autowired
    private FastApiService fastApiService;
    @Autowired
    private VideoService videoService;
    @Autowired
    private MessageClassService messageClassService;

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
                cursorTime = data.getTime();
                cursorId = data.getId();
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


    private CursorData decodeCursor(String cursor) {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = raw.split("\\|");
              return CursorData.builder()
                    .time(LocalDateTime.parse(parts[0]))
                    .id(Long.parseLong(parts[1]))
                    .build();

        } catch (Exception e) {
            throw new InvalidSourceException("Invalid pagination cursor");
        }
    }

    @Transactional
    public SendMessageResponseDTO sendMessage(Long sessionId, String userMessage, String token) {
        ChatSession chatSession =  chatSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        User user = jwtService.extractUser(token);
        if (!chatSession.getUser().getId().equals(user.getId())) {
            throw new AuthenticationException("Unauthorized access to chat session");
        }
        
        chatSession.setLastMessageAt(LocalDateTime.now());

        Message message = messageService.createMessage(chatSession, userMessage, MessageSender.USER);

        List<Message> messages = chatSession.getMessages();
        boolean isFirstMessage = messages.isEmpty();
        BotQueryRequestDTO botQueryRequestDTO = mapToBotQueryRequestDTO(chatSession, message, isFirstMessage);

        System.out.println("Sending BotQueryRequestDTO to FastAPI: " + botQueryRequestDTO);

        BotQueryResponseDTO botQueryResponseDTO = fastApiService.processChat(botQueryRequestDTO);

        if (isFirstMessage && botQueryResponseDTO.getSessionTitle() != null) {
            chatSession.setSessionTopic(botQueryResponseDTO.getSessionTitle());
        }

        Message response = messageService.createMessage(
                chatSession, botQueryResponseDTO.getAnswer(), MessageSender.BOT);

        messages.add(message);
        messages.add(response);
        chatSession.setMessages(messages);
        chatSessionRepo.save(chatSession);

        return SendMessageResponseDTO.builder()
                .sessionId(chatSession.getId())
                .sessionTitle(chatSession.getSessionTopic())
                .userMessage(mapToMessageDto(message))
                .aiResponse(mapToMessageDto(response))
                .sources(mapToSource(botQueryResponseDTO.getSources()))
                .build();
    }

    private BotQueryRequestDTO mapToBotQueryRequestDTO(ChatSession chatSession, Message userMessage, boolean isFirstMessage) {
        List<BotQueryRequestDTO.ConversationHistoryDTO> conversationHistory = new ArrayList<>();
        List<Message> allMessages = chatSession.getMessages();
        int startIndex = Math.max(0, allMessages.size() - 10);
        for (int i = startIndex; i < allMessages.size(); i++) {
            Message msg = allMessages.get(i);
            conversationHistory.add(
                    new BotQueryRequestDTO.ConversationHistoryDTO(
                            msg.getSender().toString(), msg.getContent()
                    )
            );
        }

        List<MessageClassResponseDTO> messageClasses = messageClassService.getAllMessageClassesByUserChatbot(
                chatSession.getChatbot()
        );

        return BotQueryRequestDTO.builder()
                .chatbotId(chatSession.getChatbot().getId().toString())
                .messageId(userMessage.getId())
                .query(userMessage.getContent())
                .conversationHistory(conversationHistory)
                .firstMessage(isFirstMessage)
                .config(
                        BotQueryRequestDTO.ConfigDTO.builder()
                                .chatbotName(chatSession.getChatbot().getConfig().getName())
                                .talkLikeMe(chatSession.getChatbot().getConfig().isTalkLikeMe())
                                .tone(chatSession.getChatbot().getConfig().getTone() != null ? chatSession.getChatbot().getConfig().getTone().name() : null)
                                .verbosity(chatSession.getChatbot().getConfig().getVerbosity() != null ? chatSession.getChatbot().getConfig().getVerbosity().name() : null)
                                .formality(chatSession.getChatbot().getConfig().getFormality() != null ? chatSession.getChatbot().getConfig().getFormality().name() : null)
                                .build()
                )
                .messageClasses(messageClasses)
                .build();
    }

    private List<SendMessageResponseDTO.Source> mapToSource(List<BotQueryResponseDTO.SourceDTO> sources) {
        if (sources == null) return new ArrayList<>();
        List<SendMessageResponseDTO.Source> sourceList = new ArrayList<>();
        for (BotQueryResponseDTO.SourceDTO sourceDTO : sources) {
            sourceList.add(
                    SendMessageResponseDTO.Source.builder()
                            .videoId(sourceDTO.getVideoId())
                            .videoTitle(sourceDTO.getVideoTitle())
                            .youtubeUrl(sourceDTO.getYoutubeUrl())
                            .thumbnailUrl(videoService.getThumbnailByYoutubeVideoId(sourceDTO.getVideoId()))
                            .build()
            );
        }
        return sourceList;
    }

    private MessageDto mapToMessageDto(Message message) {
        if (message == null) return null;
        return MessageDto.builder()
                .id(message.getId())
                .message(message.getContent())
                .sender(message.getSender())
                .messageStatus(message.getStatus())
                .sentAt(message.getSentAt())
                .messageClass(message.getMessageClass() != null ? message.getMessageClass().getName() : null)
                .build();
    }

    public List<MessageDto> getSessionMessages(Long sessionId, String token) {
        User user = jwtService.extractUser(token);
        ChatSession chatSession = chatSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (!chatSession.getUser().getId().equals(user.getId())) {
            throw new AuthenticationException("Unauthorized access to chat session");
        }
        List<MessageDto> messageDtos = new ArrayList<>();
        for (Message message : chatSession.getMessages()) {
            messageDtos.add(mapToMessageDto(message));
        }
        return messageDtos;
    }
}
