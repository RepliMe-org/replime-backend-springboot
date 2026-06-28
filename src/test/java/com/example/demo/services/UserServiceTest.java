package com.example.demo.services;

import com.example.demo.dtos.UserInfoResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotConfig;
import com.example.demo.entities.User;
import com.example.demo.entities.utils.ChatSessionStatus;
import com.example.demo.entities.utils.Role;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatSessionRepo;
import com.example.demo.repos.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for user list mapping and admin promotion,
// verifying influencer chatbot enrichment, session counts, save side effects, and not-found failures.
class UserServiceTest {

    @Test
    void getAllUsersMapsUsersWithSessionCountsAndInfluencerChatbotName() {
        UserRepo userRepo = mock(UserRepo.class);
        ChatbotService chatbotService = mock(ChatbotService.class);
        ChatSessionRepo chatSessionRepo = mock(ChatSessionRepo.class);
        UserService service = service(userRepo, chatbotService, chatSessionRepo);
        User normalUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .role(Role.USER)
                .build();
        User influencer = User.builder()
                .id(2L)
                .email("creator@example.com")
                .role(Role.INFLUENCER)
                .build();
        Chatbot chatbot = Chatbot.builder()
                .config(ChatbotConfig.builder().name("Creator Bot").build())
                .build();
        when(userRepo.findAll()).thenReturn(List.of(normalUser, influencer));
        when(chatbotService.getChatbotByUser(influencer)).thenReturn(chatbot);
        when(chatSessionRepo.countByUserIdAndStatusNot(1L, ChatSessionStatus.DELETED)).thenReturn(3L);
        when(chatSessionRepo.countByUserIdAndStatusNot(2L, ChatSessionStatus.DELETED)).thenReturn(5L);

        List<UserInfoResponseDTO> users = service.getAllUsers();

        assertEquals(2, users.size());
        assertEquals("user@example.com", users.get(0).getEmail());
        assertEquals(Role.USER, users.get(0).getRole());
        assertNull(users.get(0).getChatbotName());
        assertEquals(3L, users.get(0).getConversationsCount());
        assertEquals("creator@example.com", users.get(1).getEmail());
        assertEquals(Role.INFLUENCER, users.get(1).getRole());
        assertEquals("Creator Bot", users.get(1).getChatbotName());
        assertEquals(5L, users.get(1).getConversationsCount());
    }

    @Test
    void promoteToAdminUpdatesUserRoleAndSaves() {
        UserRepo userRepo = mock(UserRepo.class);
        UserService service = service(userRepo, mock(ChatbotService.class), mock(ChatSessionRepo.class));
        User user = User.builder().email("user@example.com").role(Role.USER).build();
        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        service.promoteToAdmin("user@example.com");

        assertEquals(Role.ADMIN, user.getRole());
        verify(userRepo).save(user);
    }

    @Test
    void promoteToAdminThrowsWhenUserDoesNotExist() {
        UserRepo userRepo = mock(UserRepo.class);
        UserService service = service(userRepo, mock(ChatbotService.class), mock(ChatSessionRepo.class));
        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.promoteToAdmin("missing@example.com"));

        assertEquals("User not found with email: missing@example.com", exception.getMessage());
    }

    private static UserService service(
            UserRepo userRepo,
            ChatbotService chatbotService,
            ChatSessionRepo chatSessionRepo
    ) {
        UserService service = new UserService();
        ReflectionTestUtils.setField(service, "userRepo", userRepo);
        ReflectionTestUtils.setField(service, "chatbotService", chatbotService);
        ReflectionTestUtils.setField(service, "chatSessionRepo", chatSessionRepo);
        return service;
    }
}
