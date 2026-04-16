package com.example.demo.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class FastApiService {

    private final WebClient webClient;

    public FastApiService(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("http://localhost:8000").build();
    }

    public Map<String, Object> callFastApi() {
        return webClient.get()
                .uri("/api/v1/base/")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }
}