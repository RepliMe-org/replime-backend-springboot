package com.example.demo.services;

import com.example.demo.dtos.InfluencerMessageClassesDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.utils.MessageClassType;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotRepo;
import com.example.demo.repos.MessageClassRepo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for message-class listing, context building, creation,
// assignment, removal, deletion, validation, duplicate conflicts, and missing class failures.
class MessageClassServiceTest {

    @Test
    void getAllSystemMessageClassesForCategoryMapsActiveSystemClasses() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        when(repo.findByCategoryIdAndTypeAndIsActiveTrue(1L, MessageClassType.SYSTEM))
                .thenReturn(List.of(MessageClass.builder().id(3L).name("Pricing").build()));

        List<MessageClassResponseDTO> response = service.getAllSystemMessageClassesForCategory(1L);

        assertEquals(1, response.size());
        assertEquals(3L, response.get(0).getId());
        assertEquals("Pricing", response.get(0).getName());
    }

    @Test
    void getInfluencerClassificationContextSplitsPickedAvailableAndCustomClasses() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        ChatbotCategory category = ChatbotCategory.builder().id(1L).name("Support").build();
        Chatbot chatbot = Chatbot.builder().category(category).build();
        MessageClass picked = MessageClass.builder().id(1L).name("Pricing").chatbots(new HashSet<>(List.of(chatbot))).build();
        MessageClass available = MessageClass.builder().id(2L).name("Refunds").chatbots(new HashSet<>()).build();
        MessageClass custom = MessageClass.builder().id(3L).name("Custom").type(MessageClassType.CUSTOM).build();
        when(repo.findByCategoryIdAndTypeAndIsActiveTrue(1L, MessageClassType.SYSTEM))
                .thenReturn(List.of(picked, available));
        when(repo.findByChatbotsContainingAndType(chatbot, MessageClassType.CUSTOM)).thenReturn(List.of(custom));

        InfluencerMessageClassesDTO response = service.getInfluencerClassificationContext(chatbot);

        assertEquals("Support", response.getCategory().getName());
        assertEquals("Pricing", response.getPickedClasses().get(0).getName());
        assertEquals("Refunds", response.getAvailableClasses().get(0).getName());
        assertEquals("Custom", response.getCustomClasses().get(0).getName());
    }

    @Test
    void createMessageClassForCategoryThrowsConflictForDuplicateName() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        ChatbotCategory category = ChatbotCategory.builder().id(1L).build();
        when(repo.existsByCategoryIdAndName(1L, "Pricing")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createMessageClassForCategory(category, List.of("Pricing")));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void assignClassesToChatbotAddsClassesAndSavesBothSides() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageClassService service = service(repo, chatbotRepo, mock(MessageService.class));
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>()).build();
        MessageClass messageClass = MessageClass.builder().id(1L).name("Pricing").chatbots(new HashSet<>()).build();
        when(repo.findByIdIn(List.of(1L))).thenReturn(List.of(messageClass));

        List<MessageClassResponseDTO> response = service.assignClassesToChatbot(List.of(1L), chatbot);

        assertTrue(messageClass.getChatbots().contains(chatbot));
        assertTrue(chatbot.getMessageClasses().contains(messageClass));
        assertEquals("Pricing", response.get(0).getName());
        verify(repo).save(messageClass);
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void removeSystemMessageClassUnlinksBothSides() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageClassService service = service(repo, chatbotRepo, mock(MessageService.class));
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>()).build();
        MessageClass messageClass = MessageClass.builder()
                .id(1L)
                .type(MessageClassType.SYSTEM)
                .chatbots(new HashSet<>(List.of(chatbot)))
                .build();
        chatbot.getMessageClasses().add(messageClass);
        when(repo.findById(1L)).thenReturn(Optional.of(messageClass));

        service.removeMessageClassFromChatbot(1L, chatbot);

        assertFalse(chatbot.getMessageClasses().contains(messageClass));
        assertFalse(messageClass.getChatbots().contains(chatbot));
        verify(repo).save(messageClass);
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void removeCustomMessageClassDeactivatesWhenMessagesReferenceIt() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageService messageService = mock(MessageService.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), messageService);
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>()).build();
        MessageClass messageClass = MessageClass.builder().id(1L).type(MessageClassType.CUSTOM).build();
        chatbot.getMessageClasses().add(messageClass);
        when(repo.findById(1L)).thenReturn(Optional.of(messageClass));
        when(messageService.existsByMessageClassId(1L)).thenReturn(true);

        service.removeMessageClassFromChatbot(1L, chatbot);

        assertFalse(messageClass.isActive());
        verify(repo).save(messageClass);
    }

    @Test
    void getMessageClassByIdThrowsWhenMissing() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        when(repo.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getMessageClassById(99L));

        assertEquals("Message class not found with id: 99", exception.getMessage());
    }

    @Test
    void isMessageClassIdValidForChatbotChecksCategoryIdentity() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        ChatbotCategory category = ChatbotCategory.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().category(category).build();
        when(repo.findById(5L)).thenReturn(Optional.of(MessageClass.builder().category(category).build()));

        assertTrue(service.isMessageClassIdValidForChatbot(5L, chatbot));
    }

    private static MessageClassService service(
            MessageClassRepo repo,
            ChatbotRepo chatbotRepo,
            MessageService messageService
    ) {
        MessageClassService service = new MessageClassService();
        ReflectionTestUtils.setField(service, "messageClassRepo", repo);
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        ReflectionTestUtils.setField(service, "messageService", messageService);
        return service;
    }
}
