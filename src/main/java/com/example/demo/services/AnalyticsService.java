package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.AnalyticsReportResponseDTO;
import com.example.demo.dtos.internal.AnalyticsProcessRequestDTO;
import com.example.demo.dtos.internal.AnalyticsProcessResponseDTO;
import com.example.demo.entities.AnalyticsReport;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Message;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.MessageIntent;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.exceptions.ResourceNotFoundException;
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

    private final AnalyticsReportRepo analyticsReportRepo;
    private final ChatbotRepo chatbotRepo;
    private final MessageRepo messageRepo;
    private final MessageSourceRepo messageSourceRepo;
    private final FastApiService fastApiService;
    private final JwtService jwtService;

    public AnalyticsReportResponseDTO generate(String token) {
        Chatbot chatbot = resolveChatbot(token);
        UUID chatbotId = chatbot.getId();

        List<Message> contentQuestions = messageRepo.findByChatbotAndSenderAndIntent(
                chatbotId, MessageSender.USER, MessageIntent.CONTENT_QUESTION);

        List<AnalyticsReport.ClassificationCount> classificationBreakdown =
                computeClassificationBreakdown(contentQuestions);

        List<AnalyticsReport.CitedVideo> mostCitedVideos =
                computeMostCitedVideos(chatbotId);

        List<AnalyticsProcessRequestDTO.QuestionDTO> questions = new ArrayList<>();
        for (Message message : contentQuestions) {
            questions.add(AnalyticsProcessRequestDTO.QuestionDTO.builder()
                    .text(message.getContent())
                    .answeredWithSources(message.getAnsweredWithSources())
                    .build());
        }

        AnalyticsProcessRequestDTO requestDTO = AnalyticsProcessRequestDTO.builder()
                .chatbotId(chatbotId.toString())
                .description(chatbot.getConfig() != null ? chatbot.getConfig().getDescription() : null)
                .questions(questions)
                .build();

        AnalyticsProcessResponseDTO aiResponse = fastApiService.processAnalytics(requestDTO);

        AnalyticsReport report = AnalyticsReport.builder()
                .chatbot(chatbot)
                .generatedAt(LocalDateTime.now())
                .classificationBreakdown(classificationBreakdown)
                .mostAskedClusters(aiResponse != null ? aiResponse.getMostAskedClusters() : null)
                .executiveSummary(aiResponse != null ? aiResponse.getExecutiveSummary() : null)
                .contentGaps(aiResponse != null ? aiResponse.getContentGaps() : null)
                .mostCitedVideos(mostCitedVideos)
                .build();

        analyticsReportRepo.save(report);

        return mapToResponseDTO(report);
    }

    public List<AnalyticsReportResponseDTO> getReports(String token) {
        UUID chatbotId = resolveChatbot(token).getId();
        return analyticsReportRepo.findByChatbotIdOrderByGeneratedAtDesc(chatbotId)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public AnalyticsReportResponseDTO getLatestReport(String token) {
        UUID chatbotId = resolveChatbot(token).getId();
        return analyticsReportRepo.findFirstByChatbotIdOrderByGeneratedAtDesc(chatbotId)
                .map(this::mapToResponseDTO)
                .orElse(null);
    }

    private Chatbot resolveChatbot(String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            throw new ResourceNotFoundException("Chatbot not found for influencer");
        }
        return chatbot;
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

    private AnalyticsReportResponseDTO mapToResponseDTO(AnalyticsReport report) {
        return AnalyticsReportResponseDTO.builder()
                .id(report.getId())
                .generatedAt(report.getGeneratedAt())
                .classificationBreakdown(report.getClassificationBreakdown())
                .mostAskedClusters(report.getMostAskedClusters())
                .executiveSummary(report.getExecutiveSummary())
                .contentGaps(report.getContentGaps())
                .mostCitedVideos(report.getMostCitedVideos())
                .build();
    }
}
