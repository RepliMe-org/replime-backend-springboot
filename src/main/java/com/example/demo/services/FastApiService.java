package com.example.demo.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.demo.dtos.internal.VideoIndexRequestDTO;

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

    public Map<String, Object> deleteVideoChunks(String videoId, String chatbotId) {
        Map<String, String> body = Map.of("chatbot_id", chatbotId);

        return webClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/internal/videos/{video_id}", videoId)
                .header("X-INTERNAL-TOKEN", X_TOKEN)
                .bodyValue(body)
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

}