package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.*;
import com.example.demo.entities.*;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.entities.utils.SourceType;
import com.example.demo.exceptions.InvalidSourceException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.exceptions.TrainingSourceException;
import com.example.demo.repos.ChatbotCategoryRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatbotService {

    @Autowired
    private ChatbotRepo chatbotRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ChatbotCategoryRepo chatbotCategoryRepo;

    @Autowired
    private InfluencerVerificationRepo influencerVerificationRepo;

    @Autowired
    private MessageClassService messageClassService;
    @Autowired
    private TrainingSourceService trainingSourceService;
    @Autowired
    private YoutubeClientService youtubeClientService;

    public void createChatbot(User user) {
        Chatbot chatbot = Chatbot.builder()
            .influencer(user)
            .status(ChatbotStatus.CONFIGURING)
            .createdAt(LocalDateTime.now())
            .build();
        chatbotRepo.save(chatbot);
        System.out.println("Created chatbot " + chatbot);
    }

    public ResponseEntity<List<PublicChatbotResponseDTO>> getPublicChatbots() {
        List<Chatbot> chatbots = chatbotRepo.findAllByIsPublicTrue();
        List<PublicChatbotResponseDTO> browseDTOs = chatbots
            .stream()
            .map(this::mapToPublicChatbotResponseDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(browseDTOs);
    }

    public ResponseEntity<List<InfluencerChatbotResponseDTO>> getAllChatbots() {
        List<Chatbot> chatbots = chatbotRepo.findAll();
        List<InfluencerChatbotResponseDTO> responseDTOs = chatbots
            .stream()
            .map(this::mapToInfluencerChatbotResponseDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responseDTOs);
    }

    private PublicChatbotResponseDTO mapToPublicChatbotResponseDTO(
        Chatbot chatbot
    ) {
        return PublicChatbotResponseDTO.builder()
            .id(chatbot.getId())
            .influencerUsername(chatbot.getInfluencer().getUsername())
            .chatbotName(
                chatbot.getConfig() != null ? chatbot.getConfig().getName() : ""
            )
            .chatbotDescription(
                chatbot.getConfig() != null
                    ? chatbot.getConfig().getDescription()
                    : ""
            )
            .greetingMessage(
                chatbot.getConfig() != null
                    ? chatbot.getConfig().getGreetingMessage()
                    : ""
            )
            .status(chatbot.getStatus())
            .build();
    }

    private InfluencerChatbotResponseDTO mapToInfluencerChatbotResponseDTO(
        Chatbot chatbot
    ) {
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
            .tone(config != null ? config.getTone() : null)
            .verbosity(config != null ? config.getVerbosity() : null)
            .formality(config != null ? config.getFormality() : null)
            .talkLikeMe(config != null && config.isTalkLikeMe())
            .configCreatedAt(config != null ? config.getCreatedAt() : null)
            .build();
    }

    private AdminChatbotResponseDTO mapToAdminChatbotResponseDTO(
        Chatbot chatbot
    ) {
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
        if (id == null) {
            return ResponseEntity.badRequest().build();
        }
        return chatbotRepo
            .findById(id)
            .map(this::mapToPublicChatbotResponseDTO)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    public ResponseEntity<InfluencerChatbotResponseDTO> getInfluencerChatbot(
        String token
    ) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToInfluencerChatbotResponseDTO(chatbot));
    }

    public ResponseEntity<
        List<AdminChatbotResponseDTO>
    > getAllChatbotsForAdmin() {
        List<Chatbot> chatbots = chatbotRepo.findAll();
        List<AdminChatbotResponseDTO> responseDTOs = chatbots
            .stream()
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
            ChatbotStatus newStatus = ChatbotStatus.valueOf(
                status.toUpperCase()
            );
            chatbot.setStatus(newStatus);
            chatbotRepo.save(chatbot);
            return ResponseEntity.ok("Chatbot status updated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid status value");
        }
    }

    public ResponseEntity<String> updateChatbotVisibility(
        UUID id,
        boolean isPublic
    ) {
        Chatbot chatbot = chatbotRepo.findById(id).orElse(null);
        if (chatbot == null) {
            return ResponseEntity.notFound().build();
        }
        chatbot.setPublic(isPublic);
        chatbotRepo.save(chatbot);
        return ResponseEntity.ok("Chatbot visibility updated successfully");
    }

    public ResponseEntity<ChatbotStatus> getChatbotStatus(String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chatbot.getStatus());
    }

    public void assignCategory(Long categoryId, String token) {
        User user = jwtService.extractUser(token);
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());

        ChatbotCategory category = chatbotCategoryRepo
            .findById(categoryId)
            .orElseThrow(() ->
                new RuntimeException("Chatbot category not found")
            );

        chatbot.setCategory(category);
        chatbotRepo.save(chatbot);
    }

    public Chatbot getChatbotByUser(User user) {
        //return chatbot by user id or else throw resource not found exception
        Chatbot chatbot = chatbotRepo.findByInfluencerId(user.getId());
        if (chatbot == null) {
            throw new ResourceNotFoundException(
                "Chatbot not found for influencer"
            );
        }
        return chatbot;
    }

    public List<MessageClassResponseDTO> getAllMessageClassesAssignedToChatbot(
        String token
    ) {
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = getChatbotByUser(user);

        if (chatbot.getCategory() == null) {
            throw new ResourceNotFoundException(
                "Chatbot category not yet set for influencer"
            );
        }
        return messageClassService.getAllMessageClassesByUserChatbot(chatbot);
    }

    public void chooseMessageClassesForChatbot(
        List<Long> messageClassIds,
        String token
    ) {
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = getChatbotByUser(user);
        messageClassService.assignClassesToChatbot(messageClassIds, chatbot);
    }

    public void createMessageClassesForSpecificChatbot(
            String token, List<String> messageClassesNames
    ) {
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = getChatbotByUser(user);
        messageClassService.createCustomMessageClassesForChatbot(chatbot, messageClassesNames);
    }

    public void deleteMessageClassFromChatbot(Long messageClassId, String token) {
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = getChatbotByUser(user);
        messageClassService.deleteMessageClassfromChatbot(messageClassId,chatbot);
    }

    @Transactional
    public void addTrainingSourceToChatbot(TrainingSourceRequestDTO sourceRequest, String token) {
        User user = jwtService.extractUser(token.substring(7));
        Chatbot chatbot = getChatbotByUser(user);
        if (sourceRequest.getSourceType() == SourceType.LAST_N){
            trainingSourceService.addTrainingSourceToChatbot(sourceRequest, chatbot);
            return;
        }

        if (isThisSourceBelongsToVerifiedChannel(user, sourceRequest)) {
            trainingSourceService.addTrainingSourceToChatbot(sourceRequest, chatbot);
        } else {
            throw new TrainingSourceException(
                "NOT_YOUR_VIDEO",
                "This video does not belong to your channel",
                org.springframework.http.HttpStatus.FORBIDDEN
            );
        }
    }

    private boolean isThisSourceBelongsToVerifiedChannel(
            User user, TrainingSourceRequestDTO sourceRequest) {
        if (sourceRequest.getSourceType() == SourceType.LAST_N) {
            return true;
        }

        String url = sourceRequest.getSourceValue();
        if (url == null) return false;

        return influencerVerificationRepo
                .findByUserAndStatusIn(user, List.of(VerificationStatus.VERIFIED))
                .map(verification -> {
                    // check if the url starts with or contains the channelUrl or uses channelId if applicable
                    // simplified check: just check if the url contains the channelUrl or vice versa
                    if (verification.getChannelUrl() != null && url.contains(verification.getChannelUrl())) {
                        return true;
                    }
                    if (verification.getChannelId() != null && url.contains(verification.getChannelId())) {
                        return true;
                    }

                    if (verification.getChannelId() != null) {
                        try {
                            if (sourceRequest.getSourceType() == SourceType.VIDEO) {
                                return youtubeClientService.isVideoUrlFromChannel(url, verification.getChannelId());
                            } else if (sourceRequest.getSourceType() == SourceType.PLAYLIST) {
                                return youtubeClientService.isPlaylistUrlFromChannel(url, verification.getChannelId());
                            }
                        } catch (Exception e) {
                            throw new InvalidSourceException("Failed to verify source URL against YouTube API.", e);
                        }
                    }

                    return false;
                })
                .orElse(false);
    }
}
