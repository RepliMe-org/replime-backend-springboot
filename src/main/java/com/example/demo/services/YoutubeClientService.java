package com.example.demo.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YoutubeClientService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public JsonNode getChannelData(String identifier) {

        String url;

        // If identifier starts with UC → it is a channelId
        if (identifier.startsWith("UC")) {
            url = "https://www.googleapis.com/youtube/v3/channels" +
                    "?part=snippet,statistics" +
                    "&id=" + identifier +
                    "&key=" + apiKey;
        }
        // Otherwise assume it's a handle
        else {
            url = "https://www.googleapis.com/youtube/v3/channels" +
                    "?part=snippet,statistics" +
                    "&forHandle=" + identifier +
                    "&key=" + apiKey;
        }

        ResponseEntity<String> response =
                restTemplate.getForEntity(url, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YouTube response");
        }
    }

    public String extractChannelId(String url) {

        url = url.trim();

        Pattern channelPattern = Pattern.compile("/channel/(UC[\\w-]+)");
        Matcher channelMatcher = channelPattern.matcher(url);

        if (channelMatcher.find()) {
            return channelMatcher.group(1);
        }

        Pattern handlePattern = Pattern.compile("/@([\\w-]+)");
        Matcher handleMatcher = handlePattern.matcher(url);

        if (handleMatcher.find()) {
            return handleMatcher.group(1);
        }

        throw new RuntimeException("Invalid YouTube channel URL");
    }
}
