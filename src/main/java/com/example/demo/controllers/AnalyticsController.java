package com.example.demo.controllers;

import com.example.demo.dtos.AnalyticsReportResponseDTO;
import com.example.demo.dtos.ContentGapResponseDTO;
import com.example.demo.services.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/influencer/chatbot/analytics")
@PreAuthorize("hasRole('INFLUENCER')")
@Tag(
    name = "Influencer Chatbot Analytics",
    description = "Endpoints for generating and retrieving chatbot analytics reports"
)
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @PostMapping
    @Operation(description = "Generate a new analytics report for the influencer's chatbot")
    public ResponseEntity<AnalyticsReportResponseDTO> generateReport(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(analyticsService.generate(token));
    }

    @GetMapping("/latest")
    @Operation(description = "Retrieve the latest analytics report for the influencer's chatbot")
    public ResponseEntity<AnalyticsReportResponseDTO> getLatestReport(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(analyticsService.getLatestReport(token));
    }

    @GetMapping("/report")
    @Operation(description = "Retrieve an analytics report by its generation timestamp")
    public ResponseEntity<AnalyticsReportResponseDTO> getReportByGeneratedAt(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime generatedAt) {
        return ResponseEntity.ok(analyticsService.getReportByGeneratedAt(token, generatedAt));
    }

    @GetMapping("/content-gaps")
    @Operation(description = "Retrieve content gaps for a specific analytics report by its generation timestamp")
    public ResponseEntity<ContentGapResponseDTO> getContentGaps(
            @RequestHeader("Authorization") String token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime generatedAt) {
        return ResponseEntity.ok(analyticsService.getContentGaps(token, generatedAt));
    }
}
