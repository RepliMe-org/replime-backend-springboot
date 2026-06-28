package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.AnalyticsReportResponseDTO;
import com.example.demo.dtos.ContentGapResponseDTO;
import com.example.demo.dtos.internal.AnalyticsProcessResponseDTO;
import com.example.demo.entities.AnalyticsReport;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.Message;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TooManyRequestsException;
import com.example.demo.repos.AnalyticsReportRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageRepo;
import com.example.demo.repos.MessageSourceRepo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for analytics generation and retrieval,
// verifying chatbot resolution, cooldown rejection, report mapping, content gaps, classification percentages, history, and missing-report failures.
class AnalyticsServiceTest {

    @Test
    void getLatestReportReturnsNullWhenNoReportExists() {
        AnalyticsReportRepo reportRepo = mock(AnalyticsReportRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        AnalyticsService service = service(reportRepo, chatbotRepo, mock(MessageRepo.class), mock(MessageSourceRepo.class),
                mock(FastApiService.class), jwtService);
        User user = User.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(reportRepo.findFirstByChatbotIdOrderByGeneratedAtDesc(chatbot.getId())).thenReturn(Optional.empty());

        assertNull(service.getLatestReport("Bearer token"));
    }

    @Test
    void getReportByGeneratedAtThrowsWhenMissing() {
        AnalyticsReportRepo reportRepo = mock(AnalyticsReportRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        AnalyticsService service = service(reportRepo, chatbotRepo, mock(MessageRepo.class), mock(MessageSourceRepo.class),
                mock(FastApiService.class), jwtService);
        User user = User.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).build();
        LocalDateTime generatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(reportRepo.findByChatbotIdAndGeneratedAt(chatbot.getId(), generatedAt)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getReportByGeneratedAt("Bearer token", generatedAt));

        assertEquals("No analytics report found for the given timestamp", exception.getMessage());
    }

    @Test
    void generateThrowsWhenPreviousReportIsInsideCooldown() {
        AnalyticsReportRepo reportRepo = mock(AnalyticsReportRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        AnalyticsService service = service(reportRepo, chatbotRepo, mock(MessageRepo.class), mock(MessageSourceRepo.class),
                mock(FastApiService.class), jwtService);
        User user = User.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).build();
        AnalyticsReport previous = AnalyticsReport.builder()
                .generatedAt(LocalDateTime.now())
                .build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(reportRepo.findFirstByChatbotIdOrderByGeneratedAtDesc(chatbot.getId())).thenReturn(Optional.of(previous));

        TooManyRequestsException exception = assertThrows(
                TooManyRequestsException.class,
                () -> service.generate("Bearer token"));

        assertEquals("Analytics can only be generated once every 1 minute(s)", exception.getMessage());
    }

    @Test
    void generateBuildsAndSavesAnalyticsReport() {
        AnalyticsReportRepo reportRepo = mock(AnalyticsReportRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageSourceRepo sourceRepo = mock(MessageSourceRepo.class);
        FastApiService fastApiService = mock(FastApiService.class);
        JwtService jwtService = mock(JwtService.class);
        AnalyticsService service = service(reportRepo, chatbotRepo, messageRepo, sourceRepo, fastApiService, jwtService);
        User user = User.builder().id(1L).build();
        UUID chatbotId = UUID.randomUUID();
        Chatbot chatbot = Chatbot.builder()
                .id(chatbotId)
                .config(ChatbotConfig.builder().aiGeneratedDescription("AI description").build())
                .build();
        MessageClass pricing = MessageClass.builder().name("Pricing").build();
        Message classified = Message.builder().messageClass(pricing).content("price?").answeredWithSources(true).build();
        Message unclassified = Message.builder().content("other?").answeredWithSources(false).build();
        AtomicReference<AnalyticsReport> savedReport = new AtomicReference<>();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(reportRepo.findFirstByChatbotIdOrderByGeneratedAtDesc(chatbotId)).thenReturn(Optional.empty());
        when(messageRepo.findByChatbotAndSenderAndIntent(chatbotId, MessageSender.USER, MessageIntent.CONTENT_QUESTION))
                .thenReturn(List.of(classified, unclassified));
        when(messageRepo.findByChatbotAndSenderAndIntentSince(
                org.mockito.ArgumentMatchers.eq(chatbotId),
                org.mockito.ArgumentMatchers.eq(MessageSender.USER),
                org.mockito.ArgumentMatchers.eq(MessageIntent.CONTENT_QUESTION),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class))).thenReturn(List.of(classified));
        when(sourceRepo.aggregateCitedVideosByChatbot(chatbotId))
                .thenReturn(Collections.singletonList(new Object[]{"yt-1", "Video", 2L}));
        when(fastApiService.processAnalytics(org.mockito.ArgumentMatchers.any())).thenReturn(AnalyticsProcessResponseDTO.builder()
                .executiveSummary("Summary")
                .contentGaps(List.of(Map.of("gap", "topic")))
                .mostAskedClusters(List.of(Map.of("cluster", "pricing")))
                .build());
        when(reportRepo.save(org.mockito.ArgumentMatchers.any(AnalyticsReport.class)))
                .thenAnswer(invocation -> {
                    AnalyticsReport report = invocation.getArgument(0);
                    savedReport.set(report);
                    return report;
                });
        when(reportRepo.findHistoryByChatbotId(chatbotId))
                .thenReturn(Collections.singletonList(new Object[]{LocalDateTime.of(2026, 1, 1, 10, 0), 1}));

        AnalyticsReportResponseDTO response = service.generate("Bearer token");

        assertNotNull(savedReport.get());
        assertSame(chatbot, savedReport.get().getChatbot());
        assertEquals(2, savedReport.get().getClassificationBreakdown().size());
        assertEquals("Pricing", savedReport.get().getClassificationBreakdown().get(0).getMessageClass());
        assertEquals(50.0, savedReport.get().getClassificationBreakdown().get(0).getPercentage());
        assertEquals("UNCLASSIFIED", savedReport.get().getClassificationBreakdown().get(1).getMessageClass());
        assertEquals(1, savedReport.get().getContentGapCount());
        assertEquals("Summary", response.getExecutiveSummary());
        assertEquals(1, response.getGeneratedAtHistory().size());
    }

    @Test
    void getContentGapsMapsReportContentGaps() {
        AnalyticsReportRepo reportRepo = mock(AnalyticsReportRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        AnalyticsService service = service(reportRepo, chatbotRepo, mock(MessageRepo.class), mock(MessageSourceRepo.class),
                mock(FastApiService.class), jwtService);
        User user = User.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().id(UUID.randomUUID()).build();
        LocalDateTime generatedAt = LocalDateTime.of(2026, 1, 2, 10, 0);
        AnalyticsReport report = AnalyticsReport.builder()
                .generatedAt(generatedAt)
                .contentGaps(List.of(Map.of("gap", "topic")))
                .build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(reportRepo.findByChatbotIdAndGeneratedAt(chatbot.getId(), generatedAt)).thenReturn(Optional.of(report));

        ContentGapResponseDTO response = service.getContentGaps("Bearer token", generatedAt);

        assertEquals(generatedAt, response.getGeneratedAt());
        assertEquals(report.getContentGaps(), response.getContentGaps());
    }

    private static AnalyticsService service(
            AnalyticsReportRepo reportRepo,
            ChatbotRepo chatbotRepo,
            MessageRepo messageRepo,
            MessageSourceRepo sourceRepo,
            FastApiService fastApiService,
            JwtService jwtService
    ) {
        return new AnalyticsService(reportRepo, chatbotRepo, messageRepo, sourceRepo, fastApiService, jwtService);
    }
}
