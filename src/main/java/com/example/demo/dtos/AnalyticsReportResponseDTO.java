package com.example.demo.dtos;

import com.example.demo.entities.AnalyticsReport;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportResponseDTO {

    private Long id;

    private LocalDateTime generatedAt;

    private List<AnalyticsReport.ClassificationCount> classificationBreakdown;

    private JsonNode mostAskedClusters;

    private String executiveSummary;

    private JsonNode contentGaps;

    private List<AnalyticsReport.CitedVideo> mostCitedVideos;
}
