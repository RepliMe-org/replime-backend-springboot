package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.User;
import com.example.demo.entities.VerificationStatus;
import com.example.demo.exceptions.VerificationException;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.example.demo.repos.UserRepo;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class InfluencerVerificationService {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepo userRepo;
    @Autowired
    private YoutubeClientService youtubeClientService;
    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;

    @Transactional
    public String requestVerification(String channelUrl, String token) {

        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        String userEmail = jwtService.extractUsername(jwtToken);

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user already has a pending or verified request
        influencerVerificationRepo.findByUserAndStatusIn(user,
                java.util.List.of(VerificationStatus.PENDING, VerificationStatus.VERIFIED))
                .ifPresent(existing -> {
                    throw new VerificationException(
                            "You have already requested verification. Status: " + existing.getStatus()
                    );
                });

        String channelId = youtubeClientService.extractChannelId(channelUrl);

        JsonNode channelData = youtubeClientService.getChannelData(channelId);

        JsonNode items = channelData.get("items");

        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Channel not found");
        }

        JsonNode statistics = items.get(0).get("statistics");

        long subscriberCount = statistics.get("subscriberCount").asLong();

        long threshold = 10000;

        if (subscriberCount < threshold) {
            throw new VerificationException(
                    "Minimum required subscribers: " + threshold +
                            ". Your channel currently has: " + subscriberCount
            );
        }

        String verificationToken = generateVerificationToken();

        InfluencerVerification verification = InfluencerVerification.builder()
                .user(user)
                .channelId(channelId)
                .channelUrl(channelUrl)
                .subscriberCount(subscriberCount)
                .verificationToken(verificationToken)
                .status(VerificationStatus.PENDING)
                .requestedAt(java.time.LocalDateTime.now())
                .build();

        influencerVerificationRepo.save(verification);
        return verificationToken;
    }

    private String generateVerificationToken() {

        byte[] randomBytes = new byte[24]; // 192 bits
        new SecureRandom().nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }
}
