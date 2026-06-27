package com.example.demo.dtos;

import com.example.demo.entities.AnalyticsReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsReportResponseDTO {

    private Long id;

    private LocalDateTime generatedAt;

    private List<LocalDateTime> generatedAtHistory;

    private List<Integer> contentGapCountHistory;

    private List<AnalyticsReport.ClassificationCount> classificationBreakdown;

    private List<Map<String, Object>> mostAskedClusters;

    private String executiveSummary;

    private List<AnalyticsReport.CitedVideo> mostCitedVideos;
}
