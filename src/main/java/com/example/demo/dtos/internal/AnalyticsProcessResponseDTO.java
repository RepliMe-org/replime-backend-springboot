package com.example.demo.dtos.internal;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsProcessResponseDTO {

    private JsonNode mostAskedClusters;

    private String executiveSummary;

    private JsonNode contentGaps;
}
