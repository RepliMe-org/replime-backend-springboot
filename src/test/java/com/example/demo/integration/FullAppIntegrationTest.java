package com.example.demo.integration;

import com.example.demo.dtos.ChatbotCategoryResponseDTO;
import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.dtos.LoginResponseDTO;
import com.example.demo.dtos.PublicChatbotResponseDTO;
import com.example.demo.dtos.internal.UpdateVideoStatusRequestDTO;
import com.example.demo.entities.utils.ChatbotStatus;
import com.example.demo.entities.utils.Role;
import com.example.demo.services.AuthService;
import com.example.demo.services.ChatbotCategoryService;
import com.example.demo.services.ChatbotService;
import com.example.demo.services.VideoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:full-app-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "jwt.expiration=86400000",
        "fastapi.token=test-internal-token",
        "youtube.api.key=test-youtube-key",
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class FullAppIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private ChatbotService chatbotService;

    @MockitoBean
    private ChatbotCategoryService chatbotCategoryService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private VideoService videoService;

    @Test
    void publicChatbotsEndpointReturnsJsonThroughFullContext() throws Exception {
        UUID chatbotId = UUID.randomUUID();
        when(chatbotService.getPublicChatbots()).thenReturn(org.springframework.http.ResponseEntity.ok(List.of(
                PublicChatbotResponseDTO.builder()
                        .id(chatbotId)
                        .influencerUsername("creator@example.com")
                        .chatbotName("Creator Bot")
                        .chatbotDescription("Answers questions")
                        .categoryName("Education")
                        .status(ChatbotStatus.ACTIVE)
                        .build()
        )));

        mockMvc.perform(get("/chatbots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(chatbotId.toString()))
                .andExpect(jsonPath("$[0].influencerUsername").value("creator@example.com"))
                .andExpect(jsonPath("$[0].chatbotName").value("Creator Bot"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void publicCategoriesEndpointIsAccessibleWithoutAuthentication() throws Exception {
        when(chatbotCategoryService.getAllCategories()).thenReturn(List.of(
                ChatbotCategoryResponseDTO.builder()
                        .id(1L)
                        .name("Education")
                        .chatbotCount(3)
                        .build()
        ));

        mockMvc.perform(get("/chatbot/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Education"))
                .andExpect(jsonPath("$[0].chatbotCount").value(3));
    }

    @Test
    void loginEndpointBindsRequestBodyAndReturnsTokenJson() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        when(authService.login(any(LoginRequestDTO.class))).thenReturn(LoginResponseDTO.builder()
                .token("jwt-token")
                .username("user@example.com")
                .role(Role.USER)
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.username").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void protectedUsersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Unauthorized"));
    }

    @Test
    void internalEndpointRejectsMissingInternalTokenBeforeControllerRuns() throws Exception {
        UpdateVideoStatusRequestDTO request = new UpdateVideoStatusRequestDTO();
        request.setStatus("COMPLETED");

        mockMvc.perform(patch("/internal/update-video-status/{videoId}", "yt-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Invalid or missing X-INTERNAL-TOKEN"));
    }

    @Test
    void internalEndpointAcceptsExpectedInternalTokenAndCallsController() throws Exception {
        UpdateVideoStatusRequestDTO request = new UpdateVideoStatusRequestDTO();
        request.setStatus("COMPLETED");

        mockMvc.perform(patch("/internal/update-video-status/{videoId}", "yt-123")
                        .header("X-INTERNAL-TOKEN", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Video status updated successfully"));

        verify(videoService).updateVideoStatus(eq("yt-123"), any(UpdateVideoStatusRequestDTO.class));
    }
}
