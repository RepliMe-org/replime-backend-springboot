package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.AdminChatbotResponseDTO;
import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.dtos.InfluencerChatbotResponseDTO;
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

    @Autowired
    private JwtService jwtService;

    public void createChatbot(User user){
        Chatbot chatbot = Chatbot.builder()
                .influencer(user)
                .status(ChatbotStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();
        chatbotRepo.save(chatbot);
        System.out.println("Created chatbot " + chatbot);
    }

    public ResponseEntity<List<PublicChatbotResponseDTO>> getPublicChatbots() {
        List<Chatbot> chatbots = chatbotRepo.findAllByIsPublicTrue();
        List<PublicChatbotResponseDTO> browseDTOs = chatbots.stream()
                .map(this::mapToPublicChatbotResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(browseDTOs);
    }

    public ResponseEntity<List<InfluencerChatbotResponseDTO>> getAllChatbots() {
        List<Chatbot> chatbots = chatbotRepo.findAll();
        List<InfluencerChatbotResponseDTO> responseDTOs = chatbots.stream()
                .map(this::mapToInfluencerChatbotResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    private PublicChatbotResponseDTO mapToPublicChatbotResponseDTO(Chatbot chatbot) {
        return PublicChatbotResponseDTO.builder()
                .id(chatbot.getId())
                .influencerUsername(chatbot.getInfluencer().getUsername())
                .chatbotName(chatbot.getConfig() != null ? chatbot.getConfig().getName() : "")
                .chatbotDescription(chatbot.getConfig() != null ? chatbot.getConfig().getDescription() : "")
                .greetingMessage(chatbot.getConfig() != null ? chatbot.getConfig().getGreetingMessage() : "")
                .status(chatbot.getStatus())
                .build();
    }

    private InfluencerChatbotResponseDTO mapToInfluencerChatbotResponseDTO(Chatbot chatbot) {
        ChatbotConfig config = chatbot.getConfig();
        return InfluencerChatbotResponseDTO.builder()
                .id(chatbot.getId())
                .influencerUsername(chatbot.getInfluencer().getUsername())
                .status(chatbot.getStatus())
                .isPublic(chatbot.isPublic())
                .createdAt(chatbot.getCreatedAt())
                .configId(config != null ? config.getId() : null)
                .chatbotName(config != null ? config.getName() : "")
                .greetingMessage(config != null ? config.getGreetingMessage() : "")
                .chatbotDescription(config != null ? config.getDescription() : "")
                .systemPrompt(config != null ? config.getSystemPrompt() : "")
                .temperature(config != null ? config.getTemperature() : null)
                .configCreatedAt(config != null ? config.getCreatedAt() : null)
                .build();
    }

    private AdminChatbotResponseDTO mapToAdminChatbotResponseDTO(Chatbot chatbot) {
        ChatbotConfig config = chatbot.getConfig();
        return AdminChatbotResponseDTO.builder()
                .id(chatbot.getId())
                .influencerUsername(chatbot.getInfluencer().getUsername())
                .chatbotName(config != null ? config.getName() : "")
                .chatbotDescription(config != null ? config.getDescription() : "")
                .greetingMessage(config != null ? config.getGreetingMessage() : "")
                .status(chatbot.getStatus())
                .isPublic(chatbot.isPublic())
                .build();
    }

    public ResponseEntity<PublicChatbotResponseDTO> getChatbotById(UUID id) {
        return chatbotRepo.findById(id)
                .map(this::mapToPublicChatbotResponseDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<InfluencerChatbotResponseDTO> getInfluencerChatbot(String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToInfluencerChatbotResponseDTO(chatbot));
    }

    public ResponseEntity<List<AdminChatbotResponseDTO>> getAllChatbotsForAdmin() {
        List<Chatbot> chatbots = chatbotRepo.findAll();
        List<AdminChatbotResponseDTO> responseDTOs = chatbots.stream()
                .map(this::mapToAdminChatbotResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    public ResponseEntity<String> updateChatbotStatus(UUID id, String status) {
        Chatbot chatbot = chatbotRepo.findById(id).orElse(null);
        if (chatbot == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            ChatbotStatus newStatus = ChatbotStatus.valueOf(status.toUpperCase());
            chatbot.setStatus(newStatus);
            chatbotRepo.save(chatbot);
            return ResponseEntity.ok("Chatbot status updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status value");
        }
    }

    public ResponseEntity<String> updateChatbotVisibility(UUID id, boolean isPublic) {
        Chatbot chatbot = chatbotRepo.findById(id).orElse(null);
        if (chatbot == null) {
            return ResponseEntity.notFound().build();
        }
        chatbot.setPublic(isPublic);
        chatbotRepo.save(chatbot);
        return ResponseEntity.ok("Chatbot visibility updated successfully");
    }
}
