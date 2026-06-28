package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.AdminChatbotResponseDTO;
import com.example.demo.dtos.InfluencerChatbotResponseDTO;
import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.Role;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.AnalyticsReportRepo;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.ChatbotCategoryRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import com.example.demo.repos.MessageClassRepo;
import com.example.demo.repos.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for chatbot listing/detail/status/visibility/category helpers,
// verifying response mapping, bad-request/not-found branches, repository saves, and token-to-user resolution.
class ChatbotServiceTest {

    @Test
    void createChatbotSavesConfiguringChatbotForUser() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotService service = service(chatbotRepo);
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();

        service.createChatbot(user);

        verify(chatbotRepo).save(org.mockito.ArgumentMatchers.argThat(chatbot ->
                chatbot.getInfluencer() == user
                        && chatbot.getStatus() == ChatbotStatus.CONFIGURING
                        && chatbot.getCreatedAt() != null));
    }

    @Test
    void getPublicChatbotsMapsPublicChatbots() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        InfluencerVerificationRepo verificationRepo = mock(InfluencerVerificationRepo.class);
        ChatbotService service = service(chatbotRepo, verificationRepo);
        User influencer = User.builder().id(1L).email("creator@example.com").build();
        Chatbot chatbot = chatbot(influencer);
        when(chatbotRepo.findAllByIsPublicTrue()).thenReturn(List.of(chatbot));
        when(verificationRepo.findByUser(influencer)).thenReturn(InfluencerVerification.builder()
                .avatarUrl("avatar.jpg")
                .handle("@creator")
                .build());

        ResponseEntity<List<PublicChatbotResponseDTO>> response = service.getPublicChatbots();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().size());
        assertEquals("creator@example.com", response.getBody().get(0).getInfluencerUsername());
        assertEquals("Creator Bot", response.getBody().get(0).getChatbotName());
        assertEquals("avatar.jpg", response.getBody().get(0).getAvatarUrl());
        assertEquals("@creator", response.getBody().get(0).getChannelHandle());
        assertEquals("Education", response.getBody().get(0).getCategoryName());
    }

    @Test
    void getChatbotByIdReturnsBadRequestForNullId() {
        ChatbotService service = service(mock(ChatbotRepo.class));

        ResponseEntity<PublicChatbotResponseDTO> response = service.getChatbotById(null);

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void getChatbotByIdReturnsNotFoundWhenMissing() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotService service = service(chatbotRepo);
        UUID id = UUID.randomUUID();
        when(chatbotRepo.findById(id)).thenReturn(Optional.empty());

        ResponseEntity<PublicChatbotResponseDTO> response = service.getChatbotById(id);

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void updateChatbotStatusSavesValidStatus() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotService service = service(chatbotRepo);
        UUID id = UUID.randomUUID();
        Chatbot chatbot = Chatbot.builder().status(ChatbotStatus.CONFIGURING).build();
        when(chatbotRepo.findById(id)).thenReturn(Optional.of(chatbot));

        ResponseEntity<String> response = service.updateChatbotStatus(id, "active");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Chatbot status updated successfully", response.getBody());
        assertEquals(ChatbotStatus.ACTIVE, chatbot.getStatus());
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void updateChatbotStatusReturnsBadRequestForInvalidStatus() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotService service = service(chatbotRepo);
        UUID id = UUID.randomUUID();
        when(chatbotRepo.findById(id)).thenReturn(Optional.of(Chatbot.builder().build()));

        ResponseEntity<String> response = service.updateChatbotStatus(id, "not-a-status");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid status value", response.getBody());
    }

    @Test
    void updateChatbotVisibilitySavesPublicFlag() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotService service = service(chatbotRepo);
        UUID id = UUID.randomUUID();
        Chatbot chatbot = Chatbot.builder().isPublic(false).build();
        when(chatbotRepo.findById(id)).thenReturn(Optional.of(chatbot));

        ResponseEntity<String> response = service.updateChatbotVisibility(id, true);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(chatbot.isPublic());
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void getInfluencerChatbotReturnsNotFoundWhenMissing() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatbotService service = service(chatbotRepo);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        User user = User.builder().id(1L).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(null);

        ResponseEntity<InfluencerChatbotResponseDTO> response = service.getInfluencerChatbot("Bearer token");

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void getChatbotStatusReturnsStatusForInfluencerChatbot() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatbotService service = service(chatbotRepo);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        User user = User.builder().id(1L).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(Chatbot.builder().status(ChatbotStatus.ACTIVE).build());

        ResponseEntity<ChatbotStatus> response = service.getChatbotStatus("Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(ChatbotStatus.ACTIVE, response.getBody());
    }

    @Test
    void assignCategorySetsCategoryAndSavesChatbot() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatbotService service = service(chatbotRepo);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        ReflectionTestUtils.setField(service, "chatbotCategoryRepo", categoryRepo);
        User user = User.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().build();
        ChatbotCategory category = ChatbotCategory.builder().id(3L).name("Education").build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(categoryRepo.findById(3L)).thenReturn(Optional.of(category));

        service.assignCategory(3L, "Bearer token");

        assertSame(category, chatbot.getCategory());
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void getChatbotByUserThrowsWhenMissing() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotService service = service(chatbotRepo);
        User user = User.builder().id(1L).build();
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getChatbotByUser(user));

        assertEquals("Chatbot not found for influencer", exception.getMessage());
    }

    private static Chatbot chatbot(User influencer) {
        return Chatbot.builder()
                .id(UUID.randomUUID())
                .influencer(influencer)
                .status(ChatbotStatus.ACTIVE)
                .isPublic(true)
                .category(ChatbotCategory.builder().name("Education").build())
                .config(ChatbotConfig.builder()
                        .name("Creator Bot")
                        .description("Answers questions")
                        .greetingMessage("Hi")
                        .build())
                .build();
    }

    private static ChatbotService service(ChatbotRepo chatbotRepo) {
        return service(chatbotRepo, mock(InfluencerVerificationRepo.class));
    }

    private static ChatbotService service(ChatbotRepo chatbotRepo, InfluencerVerificationRepo verificationRepo) {
        ChatbotService service = new ChatbotService();
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        ReflectionTestUtils.setField(service, "influencerVerificationRepo", verificationRepo);
        ReflectionTestUtils.setField(service, "analyticsReportRepo", mock(AnalyticsReportRepo.class));
        ReflectionTestUtils.setField(service, "chatSessionRepo", mock(ChatSessionRepo.class));
        ReflectionTestUtils.setField(service, "messageClassRepo", mock(MessageClassRepo.class));
        ReflectionTestUtils.setField(service, "userRepo", mock(UserRepo.class));
        ReflectionTestUtils.setField(service, "trainingSourceService", mock(TrainingSourceService.class));
        ReflectionTestUtils.setField(service, "videoService", mock(VideoService.class));
        return service;
    }
}
