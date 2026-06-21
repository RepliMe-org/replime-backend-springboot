package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.AnalyticsReportResponseDTO;
import com.example.demo.dtos.ContentGapResponseDTO;
import com.example.demo.dtos.internal.AnalyticsProcessRequestDTO;
import com.example.demo.dtos.internal.AnalyticsProcessResponseDTO;
import com.example.demo.entities.AnalyticsReport;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Message;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TooManyRequestsException;
import com.example.demo.repos.AnalyticsReportRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageRepo;
import com.example.demo.repos.MessageSourceRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private static final long COOLDOWN_MINUTES = 1;
    private static final LocalDateTime EPOCH = LocalDateTime.of(2000, 1, 1, 0, 0);

    private final AnalyticsReportRepo analyticsReportRepo;
    private final ChatbotRepo chatbotRepo;
    private final MessageRepo messageRepo;
    private final MessageSourceRepo messageSourceRepo;
    private final FastApiService fastApiService;
    private final JwtService jwtService;

    public AnalyticsReportResponseDTO generate(String token) {
        Chatbot chatbot = resolveChatbot(token);
        UUID chatbotId = chatbot.getId();

        AnalyticsReport previous = analyticsReportRepo
                .findFirstByChatbotIdOrderByGeneratedAtDesc(chatbotId)
                .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (previous != null) {
            LocalDateTime nextAvailableAt = previous.getGeneratedAt().plusMinutes(COOLDOWN_MINUTES);
            if (now.isBefore(nextAvailableAt)) {
                throw new TooManyRequestsException(
                        "Analytics can only be generated once every " + COOLDOWN_MINUTES + " minute(s)",
                        nextAvailableAt);
            }
        }

        LocalDateTime since = previous != null ? previous.getGeneratedAt() : EPOCH;

        // Cumulative: all-time data for stable proportions
        List<Message> allContentQuestions = messageRepo.findByChatbotAndSenderAndIntent(
                chatbotId, MessageSender.USER, MessageIntent.CONTENT_QUESTION);
        List<AnalyticsReport.ClassificationCount> classificationBreakdown =
                computeClassificationBreakdown(allContentQuestions);
        List<AnalyticsReport.CitedVideo> mostCitedVideos = computeMostCitedVideos(chatbotId);

        // Incremental: only messages since last report for actionable insights
        List<Message> recentQuestions = messageRepo.findByChatbotAndSenderAndIntentSince(
                chatbotId, MessageSender.USER, MessageIntent.CONTENT_QUESTION, since);
        List<AnalyticsProcessRequestDTO.QuestionDTO> questions = new ArrayList<>();
        for (Message message : recentQuestions) {
            questions.add(AnalyticsProcessRequestDTO.QuestionDTO.builder()
                    .text(message.getContent())
                    .answeredWithSources(message.getAnsweredWithSources())
                    .build());
        }

        AnalyticsProcessRequestDTO requestDTO = AnalyticsProcessRequestDTO.builder()
                .chatbotId(chatbotId.toString())
                .description(chatbot.getConfig() != null ? chatbot.getConfig().getAiGeneratedDescription() : null)
                .questions(questions)
                .build();

        AnalyticsProcessResponseDTO aiResponse = fastApiService.processAnalytics(requestDTO);

        int contentGapCount = aiResponse != null && aiResponse.getContentGaps() != null
                ? aiResponse.getContentGaps().size() : 0;

        AnalyticsReport report = AnalyticsReport.builder()
                .chatbot(chatbot)
                .generatedAt(now)
                .classificationBreakdown(classificationBreakdown)
                .mostAskedClusters(aiResponse != null ? aiResponse.getMostAskedClusters() : null)
                .executiveSummary(aiResponse != null ? aiResponse.getExecutiveSummary() : null)
                .contentGaps(aiResponse != null ? aiResponse.getContentGaps() : null)
                .contentGapCount(contentGapCount)
                .mostCitedVideos(mostCitedVideos)
                .build();

        analyticsReportRepo.save(report);

        return mapToResponseDTO(report, getHistory(chatbotId));
    }

    public AnalyticsReportResponseDTO getLatestReport(String token) {
        UUID chatbotId = resolveChatbot(token).getId();
        AnalyticsReport report = analyticsReportRepo
                .findFirstByChatbotIdOrderByGeneratedAtDesc(chatbotId)
                .orElse(null);
        if (report == null) return null;
        return mapToResponseDTO(report, getHistory(chatbotId));
    }

    public AnalyticsReportResponseDTO getReportByGeneratedAt(String token, LocalDateTime generatedAt) {
        UUID chatbotId = resolveChatbot(token).getId();
        AnalyticsReport report = analyticsReportRepo
                .findByChatbotIdAndGeneratedAt(chatbotId, generatedAt)
                .orElseThrow(() -> new ResourceNotFoundException("No analytics report found for the given timestamp"));
        return mapToResponseDTO(report, getHistory(chatbotId));
    }

    public ContentGapResponseDTO getContentGaps(String token, LocalDateTime generatedAt) {
        UUID chatbotId = resolveChatbot(token).getId();
        AnalyticsReport report = analyticsReportRepo
                .findByChatbotIdAndGeneratedAt(chatbotId, generatedAt)
                .orElseThrow(() -> new ResourceNotFoundException("No analytics report found for the given timestamp"));
        return ContentGapResponseDTO.builder()
                .generatedAt(report.getGeneratedAt())
                .contentGaps(report.getContentGaps())
                .build();
    }

    private Chatbot resolveChatbot(String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            throw new ResourceNotFoundException("Chatbot not found for influencer");
        }
        return chatbot;
    }

    private List<Object[]> getHistory(UUID chatbotId) {
        return analyticsReportRepo.findHistoryByChatbotId(chatbotId);
    }

    private AnalyticsReportResponseDTO mapToResponseDTO(AnalyticsReport report, List<Object[]> history) {
        List<LocalDateTime> generatedAtHistory = history.stream()
                .map(row -> (LocalDateTime) row[0])
                .toList();
        List<Integer> contentGapCountHistory = history.stream()
                .map(row -> ((Number) row[1]).intValue())
                .toList();
        return AnalyticsReportResponseDTO.builder()
                .id(report.getId())
                .generatedAt(report.getGeneratedAt())
                .generatedAtHistory(generatedAtHistory)
                .contentGapCountHistory(contentGapCountHistory)
                .classificationBreakdown(report.getClassificationBreakdown())
                .mostAskedClusters(report.getMostAskedClusters())
                .executiveSummary(report.getExecutiveSummary())
                .mostCitedVideos(report.getMostCitedVideos())
                .build();
    }

    private List<AnalyticsReport.ClassificationCount> computeClassificationBreakdown(List<Message> messages) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Message message : messages) {
            String key = message.getMessageClass() != null
                    ? message.getMessageClass().getName()
                    : "UNCLASSIFIED";
            counts.merge(key, 1L, Long::sum);
        }
        long total = messages.size();
        List<AnalyticsReport.ClassificationCount> breakdown = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            double percentage = total == 0 ? 0.0
                    : Math.round((entry.getValue() * 10000.0 / total)) / 100.0;
            breakdown.add(AnalyticsReport.ClassificationCount.builder()
                    .messageClass(entry.getKey())
                    .count(entry.getValue())
                    .percentage(percentage)
                    .build());
        }
        return breakdown;
    }

    private List<AnalyticsReport.CitedVideo> computeMostCitedVideos(UUID chatbotId) {
        List<Object[]> rows = messageSourceRepo.aggregateCitedVideosByChatbot(chatbotId);
        List<AnalyticsReport.CitedVideo> citedVideos = new ArrayList<>();
        for (Object[] row : rows) {
            citedVideos.add(AnalyticsReport.CitedVideo.builder()
                    .videoId((String) row[0])
                    .title((String) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }
        return citedVideos;
    }
}
