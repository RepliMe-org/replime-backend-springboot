package com.example.demo.services;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.ChatbotConfigRequestDTO;
import com.example.demo.dtos.ChatbotConfigResponseDTO;
import com.example.demo.dtos.ChatbotConfigUpdateDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.InfluencerVerification;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.Formality;
import com.example.demo.entities.utils.Role;
import com.example.demo.entities.utils.Tone;
import com.example.demo.entities.utils.Verbosity;
import com.example.demo.entities.utils.VerificationStatus;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotConfigRepo;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.InfluencerVerificationRepo;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for chatbot config save/update flows,
// verifying missing/existing config errors, saved config fields, fetch-channel side effect, partial updates, and avatar mapping.
class ChatbotConfigServiceTest {

    @Test
    void saveChatbotConfigReturnsBadRequestWhenNoChatbotExists() {
        JwtService jwtService = mock(JwtService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                mock(ChatbotConfigRepo.class),
                jwtService,
                mock(ChatbotService.class),
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(null);

        ResponseEntity<String> response = service.saveChatbotConfig(configRequest(false), "Bearer token");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("No chatbot found for user", response.getBody());
    }

    @Test
    void saveChatbotConfigReturnsBadRequestWhenConfigAlreadyExists() {
        JwtService jwtService = mock(JwtService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                mock(ChatbotConfigRepo.class),
                jwtService,
                mock(ChatbotService.class),
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();
        Chatbot chatbot = Chatbot.builder().config(ChatbotConfig.builder().name("Existing").build()).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);

        ResponseEntity<String> response = service.saveChatbotConfig(configRequest(false), "Bearer token");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Chatbot config already exists", response.getBody());
    }

    @Test
    void saveChatbotConfigPersistsConfigUpdatesChatbotAndFetchesChannelWhenRequested() {
        ChatbotConfigRepo configRepo = mock(ChatbotConfigRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatbotService chatbotService = mock(ChatbotService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                configRepo,
                jwtService,
                chatbotService,
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();
        Chatbot chatbot = Chatbot.builder().status(ChatbotStatus.CONFIGURING).build();
        ChatbotConfigRequestDTO request = configRequest(true);
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(configRepo.save(org.mockito.ArgumentMatchers.any(ChatbotConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<String> response = service.saveChatbotConfig(request, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Chatbot config saved successfully", response.getBody());
        assertNotNull(chatbot.getConfig());
        assertEquals("Creator Bot", chatbot.getConfig().getName());
        assertEquals("Helpful bot", chatbot.getConfig().getDescription());
        assertEquals("Hello", chatbot.getConfig().getGreetingMessage());
        assertFalse(chatbot.getConfig().isTalkLikeMe());
        assertEquals(Tone.FRIENDLY, chatbot.getConfig().getTone());
        assertEquals(Verbosity.BALANCED, chatbot.getConfig().getVerbosity());
        assertEquals(Formality.CASUAL, chatbot.getConfig().getFormality());
        assertEquals(ChatbotStatus.TRAINING, chatbot.getStatus());
        verify(chatbotRepo).save(chatbot);
        verify(chatbotService).fetchChannelVideosToChatbot(chatbot, user);
    }

    @Test
    void saveChatbotConfigDoesNotFetchChannelWhenNotRequested() {
        ChatbotConfigRepo configRepo = mock(ChatbotConfigRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatbotService chatbotService = mock(ChatbotService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                configRepo,
                jwtService,
                chatbotService,
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();
        Chatbot chatbot = Chatbot.builder().status(ChatbotStatus.CONFIGURING).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(configRepo.save(org.mockito.ArgumentMatchers.any(ChatbotConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.saveChatbotConfig(configRequest(false), "Bearer token");

        verifyNoInteractions(chatbotService);
    }

    @Test
    void updateChatbotConfigReturnsBadRequestWhenChatbotMissing() {
        JwtService jwtService = mock(JwtService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                mock(ChatbotConfigRepo.class),
                jwtService,
                mock(ChatbotService.class),
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(null);

        ResponseEntity<ChatbotConfigResponseDTO> response =
                service.updateChatbotConfig(new ChatbotConfigUpdateDTO(), "Bearer token");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void updateChatbotConfigAppliesPartialUpdatesAndMapsAvatar() {
        ChatbotConfigRepo configRepo = mock(ChatbotConfigRepo.class);
        JwtService jwtService = mock(JwtService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        InfluencerVerificationRepo verificationRepo = mock(InfluencerVerificationRepo.class);
        ChatbotConfigService service = service(
                configRepo,
                jwtService,
                mock(ChatbotService.class),
                chatbotRepo,
                verificationRepo);
        User user = User.builder().id(1L).role(Role.INFLUENCER).build();
        ChatbotConfig config = ChatbotConfig.builder()
                .id(3L)
                .name("Old")
                .description("Old description")
                .greetingMessage("Old greeting")
                .talkLikeMe(false)
                .tone(Tone.NEUTRAL)
                .verbosity(Verbosity.CONCISE)
                .formality(Formality.FORMAL)
                .fetchChannel(false)
                .build();
        Chatbot chatbot = Chatbot.builder().config(config).build();
        ChatbotConfigUpdateDTO request = new ChatbotConfigUpdateDTO();
        request.setName("New");
        request.setTalkLikeMe(true);
        request.setVerbosity(Verbosity.DETAILED);
        when(jwtService.extractUser("Bearer token")).thenReturn(user);
        when(chatbotRepo.findByInfluencerId(1L)).thenReturn(chatbot);
        when(configRepo.save(config)).thenReturn(config);
        when(verificationRepo.findByUserAndStatusIn(user, List.of(VerificationStatus.VERIFIED)))
                .thenReturn(Optional.of(InfluencerVerification.builder().avatarUrl("avatar.jpg").build()));

        ResponseEntity<ChatbotConfigResponseDTO> response =
                service.updateChatbotConfig(request, "Bearer token");

        assertEquals(200, response.getStatusCode().value());
        assertSame(config, ReflectionTestUtils.getField(chatbot, "config"));
        assertEquals("New", config.getName());
        assertEquals("Old description", config.getDescription());
        assertEquals("Old greeting", config.getGreetingMessage());
        assertEquals(Verbosity.DETAILED, config.getVerbosity());
        assertNull(config.getTone());
        assertNull(config.getFormality());
        assertEquals("avatar.jpg", response.getBody().getAvatarUrl());
    }

    @Test
    void updateAiGeneratedDescriptionUpdatesConfig() {
        ChatbotConfigRepo configRepo = mock(ChatbotConfigRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                configRepo,
                mock(JwtService.class),
                mock(ChatbotService.class),
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        UUID chatbotId = UUID.randomUUID();
        ChatbotConfig config = ChatbotConfig.builder()
                .name("Creator")
                .description("User description")
                .greetingMessage("Hi")
                .talkLikeMe(true)
                .build();
        Chatbot chatbot = Chatbot.builder().id(chatbotId).config(config).build();
        when(chatbotRepo.findById(chatbotId)).thenReturn(Optional.of(chatbot));
        when(configRepo.save(config)).thenReturn(config);

        service.updateAiGeneratedDescription(chatbotId, "Generated description");

        assertEquals("Generated description", config.getAiGeneratedDescription());
        verify(configRepo).save(config);
    }

    @Test
    void updateAiGeneratedDescriptionThrowsWhenChatbotMissing() {
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotConfigService service = service(
                mock(ChatbotConfigRepo.class),
                mock(JwtService.class),
                mock(ChatbotService.class),
                chatbotRepo,
                mock(InfluencerVerificationRepo.class));
        UUID chatbotId = UUID.randomUUID();
        when(chatbotRepo.findById(chatbotId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateAiGeneratedDescription(chatbotId, "Generated description"));
    }

    private static ChatbotConfigRequestDTO configRequest(boolean fetchChannel) {
        ChatbotConfigRequestDTO request = new ChatbotConfigRequestDTO();
        request.setName("Creator Bot");
        request.setDescription("Helpful bot");
        request.setGreetingMessage("Hello");
        request.setTalkLikeMe(false);
        request.setTone(Tone.FRIENDLY);
        request.setVerbosity(Verbosity.BALANCED);
        request.setFormality(Formality.CASUAL);
        request.setFetchChannel(fetchChannel);
        return request;
    }

    private static ChatbotConfigService service(
            ChatbotConfigRepo configRepo,
            JwtService jwtService,
            ChatbotService chatbotService,
            ChatbotRepo chatbotRepo,
            InfluencerVerificationRepo verificationRepo
    ) {
        ChatbotConfigService service = new ChatbotConfigService();
        ReflectionTestUtils.setField(service, "chatbotConfigRepo", configRepo);
        ReflectionTestUtils.setField(service, "jwtService", jwtService);
        ReflectionTestUtils.setField(service, "chatbotService", chatbotService);
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        ReflectionTestUtils.setField(service, "influencerVerificationRepo", verificationRepo);
        return service;
    }
}
