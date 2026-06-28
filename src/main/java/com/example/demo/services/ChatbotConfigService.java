package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ChatbotConfigResponseDTO;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotConfigRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

        config = chatbotConfigRepo.save(config);
        return ResponseEntity.ok(mapToChatbotConfigResponseDTO(config, user));
    }

    @Transactional
    public void updateAiGeneratedDescription(UUID chatbotId, String description) {
        Chatbot chatbot = chatbotRepo.findById(chatbotId)
                .orElseThrow(() -> new ResourceNotFoundException("Chatbot not found with id: " + chatbotId));

        ChatbotConfig config = chatbot.getConfig();
        if (config == null) {
            throw new ResourceNotFoundException("Chatbot config not found for chatbot id: " + chatbotId);
        }

        config.setAiGeneratedDescription(description);
        chatbotConfigRepo.save(config);
    }

    private ChatbotConfigResponseDTO mapToChatbotConfigResponseDTO(ChatbotConfig config, User user) {
        if (config == null) {
            return null;
        }
        String avatarUrl = influencerVerificationRepo
                .findByUserAndStatusIn(user, List.of(VerificationStatus.VERIFIED))
                .map(verification -> verification.getAvatarUrl())
                .orElse(null);
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
                .avatarUrl(avatarUrl)
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
