package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ChatbotConfigResponseDTO;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.User;
import com.example.demo.repos.ChatbotConfigRepo;
import com.example.demo.repos.ChatbotRepo;
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

    public ResponseEntity<String> saveChatbotConfig(ChatbotConfigRequestDTO requestDTO, String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.badRequest().body("No chatbot found for user");
        }
        if (chatbot.getConfig() != null) {
            return ResponseEntity.badRequest().body("Chatbot config already exists");
        }
        ChatbotConfig config = mapToChatbotConfig(requestDTO, chatbot);
        chatbotConfigRepo.save(config);
        chatbot.setConfig(config);
        chatbot.setStatus(ChatbotStatus.TRAINING);
        chatbotRepo.save(chatbot);

        if (requestDTO.getFetchChannel()) {
            chatbotService.fetchChannelVideosToChatbot(chatbot,user);
        }
        return ResponseEntity.ok("Chatbot config saved successfully");
    }


    private ChatbotConfig mapToChatbotConfig(ChatbotConfigRequestDTO requestDTO, Chatbot chatbot) {
        if (requestDTO == null) {
            return null;
        }

        return ChatbotConfig.builder()
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
                .avatarNumber(requestDTO.getAvatarNumber())
                .build();
    }

    public ResponseEntity<ChatbotConfigResponseDTO> updateChatbotConfig(ChatbotConfigUpdateDTO requestDTO, String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.badRequest().build();
        }
        if (chatbot.getConfig() == null) {
            return ResponseEntity.badRequest().build();
        }
        ChatbotConfig config = chatbot.getConfig();

        applyConfigUpdates(requestDTO, config);
        chatbotConfigRepo.save(config);
        chatbot.setConfig(config);
        chatbotRepo.save(chatbot);

        return ResponseEntity.ok(mapToChatbotConfigResponseDTO(config));
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
                .avatarNumber(config.getAvatarNumber())
                .createdAt(config.getCreatedAt())
                .build();
    }

    private void applyConfigUpdates(ChatbotConfigUpdateDTO requestDTO, ChatbotConfig config) {
        if (requestDTO == null || config == null) {
            return;
        }

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
            if (requestDTO.getTalkLikeMe()) { // if get talkLikeMe switched on
                config.setTone(null);
                config.setFormality(null);
            }
        }
        if (requestDTO.getTone() != null) {
            config.setTone(requestDTO.getTone());
        }
        if (requestDTO.getFormality() != null) {
            config.setFormality(requestDTO.getFormality());
        }
        if (requestDTO.getTone() != null) {
            config.setTone(requestDTO.getTone());
        }

        if (requestDTO.getAvatarNumber() != null) {
            config.setAvatarNumber(requestDTO.getAvatarNumber());
        }
    }
}
