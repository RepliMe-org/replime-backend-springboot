package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.ChatbotStatus;
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
                .systemPrompt(requestDTO.getSystemPrompt())
                .temperature(requestDTO.getTemperature())
                .createdAt(LocalDateTime.now())
                .build();
    }

    public ResponseEntity<String> updateChatbotConfig(ChatbotConfigUpdateDTO requestDTO, String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.badRequest().body("No chatbot found for user");
        }
        if (chatbot.getConfig() == null) {
            return ResponseEntity.badRequest().body("Chatbot config does not exist");
        }
        ChatbotConfig config = chatbot.getConfig();

        applyConfigUpdates(requestDTO, config);
        chatbotConfigRepo.save(config);
        chatbot.setConfig(config);
        chatbotRepo.save(chatbot);
        return ResponseEntity.ok("Chatbot config updated successfully");
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
        if (requestDTO.getSystemPrompt() != null) {
            config.setSystemPrompt(requestDTO.getSystemPrompt());
        }
        if (requestDTO.getTemperature() != null) {
            config.setTemperature(requestDTO.getTemperature());
        }
    }
}
