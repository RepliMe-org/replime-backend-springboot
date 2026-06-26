package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.dtos.ChatSessionSearchResponseDTO;
import com.example.demo.dtos.SendMessageResponseDTO;
import com.example.demo.dtos.SessionListResponseDTO;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.dtos.internal.BotQueryRequestDTO;
import com.example.demo.dtos.internal.BotQueryResponseDTO;
import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.dtos.utils.MessageSourceDto;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Message;
import com.example.demo.entities.MessageSource;
import com.example.demo.entities.User;
import com.example.demo.entities.Video;
import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.entities.MessageClass;
import com.example.demo.exceptions.AuthenticationException;
import com.example.demo.exceptions.InvalidSourceException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TooManyRequestsException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageRepo;
import com.example.demo.repos.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

import com.example.demo.dtos.utils.CursorData;

@Service
@Slf4j
public class ChatSessionService {
    private static final int MIN_SEARCH_QUERY_LENGTH = 2;
    private static final int MAX_SEARCHES_PER_MINUTE = 20;

    private static final Comparator<Message> MESSAGE_CONVERSATION_ORDER = Comparator
            .comparing(Message::getSentAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Message::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final Map<Long, Deque<LocalDateTime>> searchRequestsByUser = new HashMap<>();

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
    private VideoRepository videoRepository;
    @Autowired
    private MessageClassService messageClassService;
    @Autowired
    private MessageRepo messageRepo;

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
                .sessionTopic(chatSession.getSessionTopic())
                .build();
    }

    public SessionResponseDTO getSessionDetails(Long sessionId, String token) {
        User user = jwtService.extractUser(token);
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
            try {
                CursorData data = decodeCursor(cursor);
                cursorTime = data.getTime();
                cursorId = data.getId();
                sessions = chatSessionRepo.findSessions(
                        user.getId(), chatbotId, cursorTime, cursorId, safeLimit + 1);
            } catch (InvalidSourceException e) {
                throw new InvalidSourceException("Invalid pagination cursor");
            }
        } else {
            sessions = chatSessionRepo.findFirstPage(
                    user.getId(), chatbotId, pageable);
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
            SessionListResponseDTO.SessionItem sessionItem = new SessionListResponseDTO.SessionItem();
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

        SessionListResponseDTO.PaginationInfo paginationInfo = SessionListResponseDTO.PaginationInfo.builder()
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
            // Format: "2025-05-04T10:31:00|42" then Base64-encode
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
                    StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            return CursorData.builder()
                    .time(LocalDateTime.parse(parts[0]))
                    .id(Long.parseLong(parts[1]))
                    .build();

        } catch (Exception e) {
            throw new InvalidSourceException("Invalid pagination cursor");
        }
    }

    public SendMessageResponseDTO sendMessage(Long sessionId, String userMessage, String token) {
        ChatSession chatSession = chatSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        User user = jwtService.extractUser(token);
        if (!chatSession.getUser().getId().equals(user.getId())) {
            throw new AuthenticationException("Unauthorized access to chat session");
        }

        if (chatSession.getStatus() == ChatSessionStatus.DELETED) {
            throw new ResourceNotFoundException("Chat session not found");
        }

        chatSession.setLastMessageAt(LocalDateTime.now());
        chatSessionRepo.saveAndFlush(chatSession);

        List<Message> messages = chatSession.getMessages();
        boolean isFirstMessage = messages.isEmpty();
        Message message = messageService.createMessage(chatSession, userMessage, MessageSender.USER);

        BotQueryRequestDTO botQueryRequestDTO = mapToBotQueryRequestDTO(chatSession, message, isFirstMessage);

        BotQueryResponseDTO botQueryResponseDTO = fastApiService.processChat(botQueryRequestDTO);

        if (isFirstMessage && botQueryResponseDTO.getSessionTitle() != null) {
            chatSession.setSessionTopic(botQueryResponseDTO.getSessionTitle());
        }

        Message response = messageService.createMessage(
                chatSession, botQueryResponseDTO.getAnswer(), MessageSender.BOT);

        List<MessageSource> messageSources = buildMessageSources(botQueryResponseDTO.getSources(), response);
        response.getSources().addAll(messageSources);
        response = messageService.saveMessage(response);

        message = messageService.getMessage(message.getId());
        message.setIntent(parseIntent(botQueryResponseDTO.getIntent()));
        message.setAnsweredWithSources(
                botQueryResponseDTO.getSources() != null && !botQueryResponseDTO.getSources().isEmpty());
        message = messageService.saveMessage(message);

        messages.add(message);
        messages.add(response);
        chatSession.setMessages(messages);
        chatSessionRepo.saveAndFlush(chatSession);


        return SendMessageResponseDTO.builder()
                .sessionId(chatSession.getId())
                .sessionTitle(chatSession.getSessionTopic())
                .userMessage(mapToMessageDto(message))
                .aiResponse(mapToMessageDto(response))

                .build();
    }

    private BotQueryRequestDTO mapToBotQueryRequestDTO(ChatSession chatSession, Message userMessage,
            boolean isFirstMessage) {
        List<BotQueryRequestDTO.ConversationHistoryDTO> conversationHistory = new ArrayList<>();
        List<Message> allMessages = getOrderedMessages(chatSession);
        int startIndex = Math.max(0, allMessages.size() - 10);
        for (int i = startIndex; i < allMessages.size(); i++) {
            Message msg = allMessages.get(i);
            conversationHistory.add(
                    new BotQueryRequestDTO.ConversationHistoryDTO(
                            msg.getSender().toString(), msg.getContent()));
        }

        List<MessageClassResponseDTO> messageClasses = messageClassService.getAllMessageClassesByUserChatbot(
                chatSession.getChatbot());

        return BotQueryRequestDTO.builder()
                .chatbotId(chatSession.getChatbot().getId().toString())
                .messageId(userMessage.getId())
                .query(userMessage.getContent())
                .conversationHistory(conversationHistory)
                .firstMessage(isFirstMessage)
                .config(
                        BotQueryRequestDTO.ConfigDTO.builder()
                                .chatbotName(chatSession.getChatbot().getConfig().getName())
                                .description(chatSession.getChatbot().getConfig().getAiGeneratedDescription())
                                .talkLikeMe(chatSession.getChatbot().getConfig().isTalkLikeMe())
                                .tone(chatSession.getChatbot().getConfig().getTone() != null
                                        ? chatSession.getChatbot().getConfig().getTone().name()
                                        : null)
                                .verbosity(chatSession.getChatbot().getConfig().getVerbosity() != null
                                        ? chatSession.getChatbot().getConfig().getVerbosity().name()
                                        : null)
                                .formality(chatSession.getChatbot().getConfig().getFormality() != null
                                        ? chatSession.getChatbot().getConfig().getFormality().name()
                                        : null)
                                .build())
                .messageClasses(messageClasses)
                .build();
    }


    private MessageIntent parseIntent(String intent) {
        if (intent == null || intent.isBlank()) return null;
        try {
            return MessageIntent.valueOf(intent.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown message intent value from AI server: {}", intent);
            return null;
        }
    }

    private List<MessageSource> buildMessageSources(
            List<BotQueryResponseDTO.SourceDTO> sources, Message botMessage) {
        if (sources == null) return new ArrayList<>();
        List<MessageSource> result = new ArrayList<>();
        for (BotQueryResponseDTO.SourceDTO s : sources) {
            videoRepository.findByYoutubeVideoId(s.getVideoId()).ifPresent(video ->
                    result.add(MessageSource.builder()
                            .message(botMessage)
                            .video(video)
                            .youtubeUrl(s.getYoutubeUrl())
                            .build())
            );
        }
        return result;
    }

    private List<Message> getOrderedMessages(ChatSession chatSession) {
        List<Message> messages = new ArrayList<>(chatSession.getMessages());
        messages.sort(MESSAGE_CONVERSATION_ORDER);
        return messages;
    }

    private MessageDto mapToMessageDto(Message message) {
        if (message == null) return null;

        List<MessageSourceDto> sourceDtos = message.getSources().stream()
                .map(ms -> MessageSourceDto.builder()
                        .videoId(ms.getVideo().getYoutubeVideoId())
                        .videoTitle(ms.getVideo().getTitle())
                        .thumbnailUrl(ms.getVideo().getThumbnailUrl())
                        .youtubeUrl(ms.getYoutubeUrl())
                        .build())
                .toList();

        return MessageDto.builder()
                .id(message.getId())
                .message(message.getContent())
                .sender(message.getSender())
                .messageStatus(message.getStatus())
                .sentAt(message.getSentAt())
                .messageClass(message.getMessageClass() != null ? message.getMessageClass().getName() : null)
                .sources(sourceDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getSessionMessages(Long sessionId, String token) {
        User user = jwtService.extractUser(token);
        ChatSession chatSession = chatSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (!chatSession.getUser().getId().equals(user.getId())) {
            throw new AuthenticationException("Unauthorized access to chat session");
        }
        List<MessageDto> messageDtos = new ArrayList<>();
        for (Message message : messageRepo.findBySessionIdOrderBySentAtAscIdAsc(sessionId)) {
            messageDtos.add(mapToMessageDto(message));
        }
        return messageDtos;
    }

    @Transactional(readOnly = true)
    public ChatSessionSearchResponseDTO searchSessions(String token, UUID chatbotId, String query) {
        User user = jwtService.extractUser(token);
        String normalizedQuery = normalizeSearchQuery(query);
        enforceSearchRateLimit(user.getId());

        List<Message> matchingMessages = messageRepo.searchUserChatbotMessages(
                user.getId(), chatbotId, normalizedQuery);

        List<ChatSessionSearchResponseDTO.SearchMatch> matches = matchingMessages.stream()
                .map(this::mapToSearchMatch)
                .toList();

        return ChatSessionSearchResponseDTO.builder()
                .query(normalizedQuery)
                .matchCount(matches.size())
                .data(matches)
                .build();
    }

    private String normalizeSearchQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new InvalidSourceException("Search query cannot be empty");
        }
        String normalizedQuery = query.trim();
        if (normalizedQuery.length() < MIN_SEARCH_QUERY_LENGTH) {
            throw new InvalidSourceException(
                    "Search query must be at least " + MIN_SEARCH_QUERY_LENGTH + " characters");
        }
        return normalizedQuery;
    }

    private void enforceSearchRateLimit(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(1);

        synchronized (searchRequestsByUser) {
            Deque<LocalDateTime> userSearches = searchRequestsByUser.computeIfAbsent(
                    userId, ignored -> new ArrayDeque<>());

            while (!userSearches.isEmpty() && !userSearches.peekFirst().isAfter(windowStart)) {
                userSearches.removeFirst();
            }

            if (userSearches.size() >= MAX_SEARCHES_PER_MINUTE) {
                LocalDateTime nextAvailableAt = userSearches.peekFirst().plusMinutes(1);
                throw new TooManyRequestsException(
                        "Search is limited to " + MAX_SEARCHES_PER_MINUTE + " request(s) per minute",
                        nextAvailableAt);
            }

            userSearches.addLast(now);
        }
    }

    private ChatSessionSearchResponseDTO.SearchMatch mapToSearchMatch(Message message) {
        ChatSession session = message.getSession();
        return ChatSessionSearchResponseDTO.SearchMatch.builder()
                .sessionId(session.getId())
                .sessionTitle(session.getSessionTopic())
                .chatbotId(session.getChatbot().getId())
                .messageId(message.getId())
                .matchedMessage(message.getContent())
                .sender(message.getSender())
                .sentAt(message.getSentAt()).build();
    }

    public void deleteSession(String token, Long sessionId) {
        User user = jwtService.extractUser(token);
        ChatSession session = chatSessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found"));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new AuthenticationException("Unauthorized access to chat session");
        }
        session.setStatus(ChatSessionStatus.DELETED);
        chatSessionRepo.save(session);
    }
}
