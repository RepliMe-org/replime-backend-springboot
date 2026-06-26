package com.example.demo.dtos.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsProcessResponseDTO {

    private List<Map<String, Object>> mostAskedClusters;

    private String executiveSummary;

    private List<Map<String, Object>> contentGaps;
}
