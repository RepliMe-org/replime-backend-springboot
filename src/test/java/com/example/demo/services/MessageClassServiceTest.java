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
import static org.mockito.Mockito.never;
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
    void getInfluencerClassificationContextHandlesMissingCategory() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        Chatbot chatbot = Chatbot.builder().build();
        when(repo.findByChatbotsContainingAndType(chatbot, MessageClassType.CUSTOM)).thenReturn(List.of());

        InfluencerMessageClassesDTO response = service.getInfluencerClassificationContext(chatbot);

        assertEquals(null, response.getCategory());
        assertTrue(response.getPickedClasses().isEmpty());
        assertTrue(response.getAvailableClasses().isEmpty());
        assertTrue(response.getCustomClasses().isEmpty());
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
    void removeCustomMessageClassDeletesWhenNoMessagesReferenceIt() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageService messageService = mock(MessageService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageClassService service = service(repo, chatbotRepo, messageService);
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>()).build();
        MessageClass messageClass = MessageClass.builder().id(1L).type(MessageClassType.CUSTOM).build();
        chatbot.getMessageClasses().add(messageClass);
        when(repo.findById(1L)).thenReturn(Optional.of(messageClass));
        when(messageService.existsByMessageClassId(1L)).thenReturn(false);

        service.removeMessageClassFromChatbot(1L, chatbot);

        assertFalse(chatbot.getMessageClasses().contains(messageClass));
        verify(repo).delete(messageClass);
        verify(repo, never()).save(messageClass);
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void createCustomMessageClassesForChatbotPersistsCustomClassesAndSavesChatbot() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageClassService service = service(repo, chatbotRepo, mock(MessageService.class));
        ChatbotCategory category = ChatbotCategory.builder().id(1L).build();
        Chatbot chatbot = Chatbot.builder().category(category).messageClasses(new HashSet<>()).build();
        when(repo.save(org.mockito.ArgumentMatchers.any(MessageClass.class))).thenAnswer(invocation -> {
            MessageClass messageClass = invocation.getArgument(0);
            messageClass.setId(7L);
            return messageClass;
        });

        service.createCustomMessageClassesForChatbot(chatbot, List.of("Custom"));

        assertEquals(1, chatbot.getMessageClasses().size());
        MessageClass saved = chatbot.getMessageClasses().iterator().next();
        assertEquals("Custom", saved.getName());
        assertEquals(MessageClassType.CUSTOM, saved.getType());
        assertEquals(category, saved.getCategory());
        assertTrue(saved.getChatbots().contains(chatbot));
        verify(chatbotRepo).save(chatbot);
    }

    @Test
    void deleteMessageClassFromCategoryDeactivatesWhenMessagesReferenceIt() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageService messageService = mock(MessageService.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), messageService);
        MessageClass messageClass = MessageClass.builder().id(1L).isActive(true).build();
        when(repo.findById(1L)).thenReturn(Optional.of(messageClass));
        when(messageService.existsByMessageClassId(1L)).thenReturn(true);

        service.deleteMessageClassFromCategory(1L);

        assertFalse(messageClass.isActive());
        verify(repo).save(messageClass);
    }

    @Test
    void deleteMessageClassFromCategoryUnlinksAndDeletesUnusedClass() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageService messageService = mock(MessageService.class);
        MessageClassService service = service(repo, chatbotRepo, messageService);
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>()).build();
        MessageClass messageClass = MessageClass.builder()
                .id(1L)
                .chatbots(new HashSet<>(List.of(chatbot)))
                .build();
        chatbot.getMessageClasses().add(messageClass);
        when(repo.findById(1L)).thenReturn(Optional.of(messageClass));
        when(messageService.existsByMessageClassId(1L)).thenReturn(false);

        service.deleteMessageClassFromCategory(1L);

        assertFalse(chatbot.getMessageClasses().contains(messageClass));
        assertTrue(messageClass.getChatbots().isEmpty());
        verify(chatbotRepo).save(chatbot);
        verify(repo).delete(messageClass);
    }

    @Test
    void deleteAllMessageClassesByCategoryHandlesReferencedAndUnusedClasses() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        MessageService messageService = mock(MessageService.class);
        MessageClassService service = service(repo, chatbotRepo, messageService);
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>()).build();
        MessageClass referenced = MessageClass.builder().id(1L).isActive(true).build();
        MessageClass unused = MessageClass.builder()
                .id(2L)
                .chatbots(new HashSet<>(List.of(chatbot)))
                .build();
        chatbot.getMessageClasses().add(unused);
        when(repo.findByCategoryId(10L)).thenReturn(List.of(referenced, unused));
        when(messageService.existsByMessageClassId(1L)).thenReturn(true);
        when(messageService.existsByMessageClassId(2L)).thenReturn(false);

        service.deleteAllMessageClassesByCategory(10L);

        assertFalse(referenced.isActive());
        assertFalse(chatbot.getMessageClasses().contains(unused));
        verify(repo).save(referenced);
        verify(chatbotRepo).save(chatbot);
        verify(repo).delete(unused);
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

    @Test
    void isMessageClassIdValidForChatbotReturnsFalseForDifferentCategoryInstance() {
        MessageClassRepo repo = mock(MessageClassRepo.class);
        MessageClassService service = service(repo, mock(ChatbotRepo.class), mock(MessageService.class));
        Chatbot chatbot = Chatbot.builder().category(ChatbotCategory.builder().id(1L).build()).build();
        when(repo.findById(5L)).thenReturn(Optional.of(MessageClass.builder()
                .category(ChatbotCategory.builder().id(1L).build())
                .build()));

        assertFalse(service.isMessageClassIdValidForChatbot(5L, chatbot));
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
