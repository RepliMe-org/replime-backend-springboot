package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ChatbotConfigResponseDTO;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.AvatarSource;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.repos.ChatbotConfigRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ChatbotConfigService {
    @Autowired
    private ChatbotConfigRepo chatbotConfigRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private ChatbotRepo chatbotRepo;

    @Autowired
    private YoutubeClientService youtubeClientService;

    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;

    public ResponseEntity<String> saveChatbotConfig(ChatbotConfigRequestDTO requestDTO, String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.badRequest().body("No chatbot found for user");
        }
        if (chatbot.getConfig() != null) {
            return ResponseEntity.badRequest().body("Chatbot config already exists");
        }
        ChatbotConfig config;
        try {
            config = mapToChatbotConfig(requestDTO, chatbot, user);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        chatbotConfigRepo.save(config);
        chatbot.setConfig(config);
        chatbot.setStatus(ChatbotStatus.TRAINING);
        chatbotRepo.save(chatbot);

        if (Boolean.TRUE.equals(requestDTO.getFetchChannel())) {
            chatbotService.fetchChannelVideosToChatbot(chatbot,user);
        }
        return ResponseEntity.ok("Chatbot config saved successfully");
    }


    private ChatbotConfig mapToChatbotConfig(ChatbotConfigRequestDTO requestDTO, Chatbot chatbot, User user) {
        if (requestDTO == null) {
            return null;
        }

        ChatbotConfig config = ChatbotConfig.builder()
                .chatbot(chatbot)
                .name(requestDTO.getName())
                .description(requestDTO.getDescription())
                .greetingMessage(requestDTO.getGreetingMessage())
                .talkLikeMe(requestDTO.getTalkLikeMe())
                .tone(requestDTO.getTone())
                .verbosity(requestDTO.getVerbosity())
                .formality(requestDTO.getFormality())
                .createdAt(LocalDateTime.now())
                .fetchChannel(requestDTO.getFetchChannel())
                .build();

        applyAvatarSelection(
                config,
                requestDTO.getFetchYoutubeProfilePicture(),
                requestDTO.getAvatarNumber(),
                user
        );
        return config;
    }

    public ResponseEntity<ChatbotConfigResponseDTO> updateChatbotConfig(ChatbotConfigUpdateDTO requestDTO, String token) {
        System.out.println("Received update request: " + requestDTO.getVerbosity());
        User user = jwtService.extractUser(token);

        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.badRequest().build();
        }
        if (chatbot.getConfig() == null) {
            return ResponseEntity.badRequest().build();
        }

        ChatbotConfig config = chatbot.getConfig();

        if (requestDTO.getName() != null) {
            config.setName(requestDTO.getName());
        }
        if (requestDTO.getDescription() != null) {
            config.setDescription(requestDTO.getDescription());
        }
        if (requestDTO.getGreetingMessage() != null) {
            config.setGreetingMessage(requestDTO.getGreetingMessage());
        }
        if (requestDTO.getTalkLikeMe() != null) {
            config.setTalkLikeMe(requestDTO.getTalkLikeMe());
        }

        if (config.isTalkLikeMe()) {
            config.setTone(null);
            config.setFormality(null);
        } else {
            if (requestDTO.getTone() != null) {
                config.setTone(requestDTO.getTone());
            }
            if (requestDTO.getFormality() != null) {
                config.setFormality(requestDTO.getFormality());
            }
        }

        if (requestDTO.getVerbosity() != null) {
            config.setVerbosity(requestDTO.getVerbosity());
        }
        if (requestDTO.getAvatarNumber() != null) {
            config.setAvatarNumber(requestDTO.getAvatarNumber());
        }
        if (requestDTO.getFetchYoutubeProfilePicture() != null) {
            try {
                applyAvatarSelection(
                        config,
                        requestDTO.getFetchYoutubeProfilePicture(),
                        requestDTO.getAvatarNumber(),
                        user
                );
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else if (requestDTO.getAvatarNumber() != null) {
            config.setAvatarSource(AvatarSource.STATIC);
            config.setAvatarUrl(null);
        }

        config = chatbotConfigRepo.save(config);
        return ResponseEntity.ok(mapToChatbotConfigResponseDTO(config));
    }

    private void applyAvatarSelection(ChatbotConfig config, Boolean fetchYoutubeProfilePicture, Integer avatarNumber, User user) {
        if (Boolean.TRUE.equals(fetchYoutubeProfilePicture)) {
            String avatarUrl = getYoutubeProfilePictureUrl(user);
            if (avatarUrl == null || avatarUrl.isBlank()) {
                throw new IllegalArgumentException("YouTube channel profile picture could not be fetched");
            }
            config.setAvatarSource(AvatarSource.YOUTUBE);
            config.setAvatarUrl(avatarUrl);
            config.setAvatarNumber(null);
            return;
        }

        config.setAvatarSource(AvatarSource.STATIC);
        config.setAvatarUrl(null);
        if (avatarNumber == null) {
            throw new IllegalArgumentException("avatarNumber is required when fetchYoutubeProfilePicture is false");
        }
        config.setAvatarNumber(avatarNumber);
    }

    private String getYoutubeProfilePictureUrl(User user) {
        InfluencerVerification verification = influencerVerificationRepo
                .findByUserAndStatusIn(user, java.util.List.of(VerificationStatus.VERIFIED))
                .orElseThrow(() -> new IllegalArgumentException("No verified YouTube channel found for user"));

        String channelIdentifier = verification.getChannelId() != null
                ? verification.getChannelId()
                : verification.getHandle();
        if (channelIdentifier == null || channelIdentifier.isBlank()) {
            throw new IllegalArgumentException("No verified YouTube channel found for user");
        }

        return youtubeClientService.getChannelProfilePictureUrl(channelIdentifier);
    }

    private ChatbotConfigResponseDTO mapToChatbotConfigResponseDTO(ChatbotConfig config) {
        if (config == null) {
            return null;
        }
        return ChatbotConfigResponseDTO.builder()
                .id(config.getId())
                .name(config.getName())
                .description(config.getDescription())
                .greetingMessage(config.getGreetingMessage())
                .talkLikeMe(config.isTalkLikeMe()) // actually isTalkLikeMe since it's boolean, let's verify if lombok generates isTalkLikeMe
                .tone(config.getTone())
                .verbosity(config.getVerbosity())
                .formality(config.getFormality())
                .fetchChannel(config.isFetchChannel())
                .fetchYoutubeProfilePicture(config.getAvatarSource() == AvatarSource.YOUTUBE)
                .avatarNumber(config.getAvatarNumber())
                .avatarUrl(config.getAvatarUrl())
                .avatarSource(config.getAvatarSource())
                .createdAt(config.getCreatedAt())
                .build();
    }

    private ChatbotConfig applyConfigUpdates(ChatbotConfigUpdateDTO requestDTO, ChatbotConfig config) {
        if (requestDTO == null || config == null) {
            return null;
        }

        return config;
    }
}
