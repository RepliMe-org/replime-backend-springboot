package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ResponseVerificationDTO;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.Role;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class InfluencerVerificationService {
    @Autowired
    private JwtService jwtService;

    @Autowired
    private YoutubeClientService youtubeClientService;
    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;

    @Transactional
    public ResponseVerificationDTO requestVerification(String channelUrl, String token) {



        User user = jwtService.extractUser(token);
        // Check if the channel url already found


        // Check if user already has a pending or verified request
        influencerVerificationRepo.findByUserAndStatusIn(user,
                List.of(VerificationStatus.PENDING, VerificationStatus.VERIFIED))
                .ifPresent(existing -> {
                    throw new VerificationException(
                            "You have already requested verification. Status: " + existing.getStatus()
                    );
                });

        String channelId = youtubeClientService.extractChannelId(channelUrl);

        InfluencerVerification isFoundChannel = influencerVerificationRepo.findByChannelId(channelId);
        if (isFoundChannel != null){
            throw new VerificationException(
                    "This channel requested verification before."
            );
        }

        JsonNode channelData = youtubeClientService.getChannelData(channelId);

        JsonNode items = channelData.get("items");

        if (items == null || items.isEmpty()) {
            throw new VerificationException("Channel not found");
        }

        JsonNode statistics = items.get(0).get("statistics");

        long subscriberCount = statistics.get("subscriberCount").asLong();

        long threshold = -1;

        if (subscriberCount < threshold) {
            throw new VerificationException(
                    "Minimum required subscribers: " + threshold +
                            ". Your channel currently has: " + subscriberCount
            );
        }

        ResponseVerificationDTO verificationTokenAndExpiry = generateVerificationToken();

        InfluencerVerification verification = InfluencerVerification.builder()
                .user(user)
                .channelId(channelId)
                .channelUrl(channelUrl)
                .subscriberCount(subscriberCount)
                .verificationToken(verificationTokenAndExpiry.getVerificationToken())
                .tokenExpirationAt(verificationTokenAndExpiry.getExpirationDateAt())
                .status(VerificationStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        influencerVerificationRepo.save(verification);
        verificationTokenAndExpiry.setMessage("Verification Requested Successfully!");
        return verificationTokenAndExpiry;
    }

    private ResponseVerificationDTO generateVerificationToken() {

        LocalDateTime expirationDate = LocalDateTime.now().plus(1, ChronoUnit.HOURS);

        byte[] randomBytes = new byte[24]; // 192 bits
        new SecureRandom().nextBytes(randomBytes);
        String verificationToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);


        return ResponseVerificationDTO.builder()
                .verificationToken(verificationToken)
                .expirationDateAt(expirationDate)
                .build();
    }

    @Transactional
    public ResponseVerificationDTO confirmVerification(String token) {

        User user = jwtService.extractUser(token);


        InfluencerVerification verification =
                influencerVerificationRepo
                        .findByUserAndStatusIn(user, List.of(VerificationStatus.PENDING))
                        .orElseThrow(() ->
                                new VerificationException("No pending verification found."));

        if (verification.getTokenExpirationAt().isBefore(LocalDateTime.now())) {

            ResponseVerificationDTO newToken = generateVerificationToken();
            newToken.setMessage("Previous token expired. New token generated.");
            verification.setVerificationToken(newToken.getVerificationToken());
            verification.setTokenExpirationAt(newToken.getExpirationDateAt());
            verification.setRequestedAt(LocalDateTime.now());
            influencerVerificationRepo.save(verification);
            return newToken;
        }

        JsonNode channelData =
                youtubeClientService.getChannelData(verification.getChannelId());

        String description = channelData
                .get("items")
                .get(0)
                .get("snippet")
                .get("description")
                .asText();

        System.out.println(description);

        String expected = "RepliMe Verification: " + verification.getVerificationToken();

        if (!description.contains(expected)) {
            throw new VerificationException("Verification token not found.");
        }

        verification.setStatus(VerificationStatus.VERIFIED);
        verification.setVerifiedAt(LocalDateTime.now());

        user.setRole(Role.INFLUENCER);
        return ResponseVerificationDTO.builder()
                .message("Influencer Verification Confirmed")
                .build();
    }
}
