package com.example.demo.controllers;

import com.example.demo.dtos.AnalyticsReportResponseDTO;
import com.example.demo.dtos.ContentGapResponseDTO;
import com.example.demo.services.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for all analytics endpoints,
// verifying token/timestamp forwarding and ResponseEntity.ok wrapping.
class AnalyticsControllerTest {

    @Test
    void generateReportReturnsServiceDto() throws Exception {
        AnalyticsController controller = new AnalyticsController();
        AnalyticsReportResponseDTO serviceResponse = AnalyticsReportResponseDTO.builder().build();
        TestAnalyticsService service = new TestAnalyticsService();
        service.reportResponse = serviceResponse;
        injectService(controller, service);

        ResponseEntity<AnalyticsReportResponseDTO> response = controller.generateReport("Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertEquals("Bearer token", service.generateToken.get());
    }

    @Test
    void getLatestReportReturnsServiceDto() throws Exception {
        AnalyticsController controller = new AnalyticsController();
        AnalyticsReportResponseDTO serviceResponse = AnalyticsReportResponseDTO.builder().build();
        TestAnalyticsService service = new TestAnalyticsService();
        service.reportResponse = serviceResponse;
        injectService(controller, service);

        ResponseEntity<AnalyticsReportResponseDTO> response = controller.getLatestReport("Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertEquals("Bearer token", service.latestToken.get());
    }

    @Test
    void getReportByGeneratedAtPassesTokenAndTimestamp() throws Exception {
        AnalyticsController controller = new AnalyticsController();
        AnalyticsReportResponseDTO serviceResponse = AnalyticsReportResponseDTO.builder().build();
        TestAnalyticsService service = new TestAnalyticsService();
        service.reportResponse = serviceResponse;
        injectService(controller, service);
        LocalDateTime generatedAt = LocalDateTime.of(2026, 1, 2, 3, 4);

        ResponseEntity<AnalyticsReportResponseDTO> response =
                controller.getReportByGeneratedAt("Bearer token", generatedAt);

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertEquals("Bearer token", service.reportToken.get());
        assertEquals(generatedAt, service.reportGeneratedAt.get());
    }

    @Test
    void getContentGapsPassesTokenAndTimestamp() throws Exception {
        AnalyticsController controller = new AnalyticsController();
        ContentGapResponseDTO serviceResponse = ContentGapResponseDTO.builder().build();
        TestAnalyticsService service = new TestAnalyticsService();
        service.contentGapResponse = serviceResponse;
        injectService(controller, service);
        LocalDateTime generatedAt = LocalDateTime.of(2026, 1, 2, 3, 4);

        ResponseEntity<ContentGapResponseDTO> response =
                controller.getContentGaps("Bearer token", generatedAt);

        assertEquals(200, response.getStatusCode().value());
        assertSame(serviceResponse, response.getBody());
        assertEquals("Bearer token", service.contentGapsToken.get());
        assertEquals(generatedAt, service.contentGapsGeneratedAt.get());
    }

    private static void injectService(AnalyticsController controller, AnalyticsService service) throws Exception {
        Field field = AnalyticsController.class.getDeclaredField("analyticsService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestAnalyticsService extends AnalyticsService {
        private AnalyticsReportResponseDTO reportResponse;
        private ContentGapResponseDTO contentGapResponse;
        private final AtomicReference<String> generateToken = new AtomicReference<>();
        private final AtomicReference<String> latestToken = new AtomicReference<>();
        private final AtomicReference<String> reportToken = new AtomicReference<>();
        private final AtomicReference<LocalDateTime> reportGeneratedAt = new AtomicReference<>();
        private final AtomicReference<String> contentGapsToken = new AtomicReference<>();
        private final AtomicReference<LocalDateTime> contentGapsGeneratedAt = new AtomicReference<>();

        private TestAnalyticsService() {
            super(null, null, null, null, null, null);
        }

        @Override
        public AnalyticsReportResponseDTO generate(String token) {
            generateToken.set(token);
            return reportResponse;
        }

        @Override
        public AnalyticsReportResponseDTO getLatestReport(String token) {
            latestToken.set(token);
            return reportResponse;
        }

        @Override
        public AnalyticsReportResponseDTO getReportByGeneratedAt(String token, LocalDateTime generatedAt) {
            reportToken.set(token);
            reportGeneratedAt.set(generatedAt);
            return reportResponse;
        }

        @Override
        public ContentGapResponseDTO getContentGaps(String token, LocalDateTime generatedAt) {
            contentGapsToken.set(token);
            contentGapsGeneratedAt.set(generatedAt);
            return contentGapResponse;
        }
    }
}
