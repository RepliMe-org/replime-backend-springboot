package com.example.demo.services;

import com.example.demo.dtos.ChatbotBrowseResponseDTO;
import com.example.demo.dtos.ChatbotResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.ChatbotStatus;
import com.example.demo.entities.User;
import com.example.demo.repos.ChatbotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatbotService {
    @Autowired
    private ChatbotRepo chatbotRepo;

    public void createChatbot(User user){
        Chatbot chatbot = Chatbot.builder()
                .influencer(user)
                .status(ChatbotStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();
        chatbotRepo.save(chatbot);
        System.out.println("Created chatbot " + chatbot);
    }

    public ResponseEntity<List<ChatbotBrowseResponseDTO>> getPublicChatbots() {
        List<Chatbot> chatbots = chatbotRepo.findAllByIsPublicTrue();
        List<ChatbotBrowseResponseDTO> browseDTOs = chatbots.stream()
                .map(this::mapToChatbotBrowseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(browseDTOs);
    }

    public ResponseEntity<List<ChatbotResponseDTO>> getAllChatbots() {
        List<Chatbot> chatbots = chatbotRepo.findAll();
        List<ChatbotResponseDTO> responseDTOs = chatbots.stream()
                .map(this::mapToChatbotResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    private ChatbotBrowseResponseDTO mapToChatbotBrowseDTO(Chatbot chatbot) {
        return ChatbotBrowseResponseDTO.builder()
                .id(chatbot.getId())
                .influencerUsername(chatbot.getInfluencer().getUsername())
                .chatbotName(chatbot.getConfig() != null ? chatbot.getConfig().getName() : "")
                .chatbotDescription(chatbot.getConfig() != null ? chatbot.getConfig().getDescription() : "")
                .greetingMessage(chatbot.getConfig() != null ? chatbot.getConfig().getGreetingMessage() : "")
                .status(chatbot.getStatus())
                .build();
    }

    private ChatbotResponseDTO mapToChatbotResponseDTO(Chatbot chatbot) {
        ChatbotConfig config = chatbot.getConfig();
        return ChatbotResponseDTO.builder()
                .id(chatbot.getId())
                .influencerUsername(chatbot.getInfluencer().getUsername())
                .status(chatbot.getStatus())
                .isPublic(chatbot.isPublic())
                .createdAt(chatbot.getCreatedAt())
                .configId(config != null ? config.getId() : null)
                .chatbotName(config != null ? config.getName() : "")
                .chatbotDescription(config != null ? config.getDescription() : "")
                .systemPrompt(config != null ? config.getSystemPrompt() : "")
                .modelName(config != null ? config.getModelName() : "")
                .temperature(config != null ? config.getTemperature() : null)
                .version(config != null ? config.getVersion() : null)
                .isActive(config != null && config.isActive())
                .configCreatedAt(config != null ? config.getCreatedAt() : null)
                .build();
    }

    public ResponseEntity<ChatbotResponseDTO> getChatbotById(UUID id) {
        return chatbotRepo.findById(id)
                .map(this::mapToChatbotResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
