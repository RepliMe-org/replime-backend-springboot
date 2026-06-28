package com.example.demo.services;

import com.example.demo.dtos.DeleteVideoRequestDTO;
import com.example.demo.dtos.internal.AnalyticsProcessRequestDTO;
import com.example.demo.dtos.internal.AnalyticsProcessResponseDTO;
import com.example.demo.dtos.internal.BotQueryRequestDTO;
import com.example.demo.dtos.internal.BotQueryResponseDTO;
import com.example.demo.dtos.internal.VideoIndexRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class FastApiService {

    private final WebClient webClient;
    @Value("${fastapi.token}")
    private String X_TOKEN;
    public FastApiService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8000/ai").build();
    }

    public Map<String, Object> callFastApi() {
        return webClient.get()
                .uri("/api/v1/health/")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public Map<String, Object> deleteVideoChunks(DeleteVideoRequestDTO deleteVideoRequestDTO) {
        return webClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/delete/video")
                .header("X-INTERNAL-TOKEN", X_TOKEN)
                .bodyValue(deleteVideoRequestDTO)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    public void indexVideos(VideoIndexRequestDTO videoIndexRequestDTO) {

        webClient.post()
                .uri("/ingest/videos")
                .header("X-INTERNAL-TOKEN", X_TOKEN)
                .bodyValue(videoIndexRequestDTO)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(error -> System.err.println("Failed to call FastAPI indexing for batch videos. " + error.getMessage()))
                .subscribe();
    }

    public BotQueryResponseDTO processChat(BotQueryRequestDTO botQueryRequestDTO) {
        return webClient.post()
                .uri("/chat/process")
                .header("X-INTERNAL-TOKEN", X_TOKEN)
                .bodyValue(botQueryRequestDTO)
                .retrieve()
                .bodyToMono(BotQueryResponseDTO.class)
                .block();
    }

    public AnalyticsProcessResponseDTO processAnalytics(AnalyticsProcessRequestDTO analyticsProcessRequestDTO) {
        return webClient.post()
                .uri("/analytics/process")
                .header("X-INTERNAL-TOKEN", X_TOKEN)
                .bodyValue(analyticsProcessRequestDTO)
                .retrieve()
                .bodyToMono(AnalyticsProcessResponseDTO.class)
                .block();
    }

}
