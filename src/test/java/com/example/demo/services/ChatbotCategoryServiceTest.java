package com.example.demo.services;

import com.example.demo.dtos.ChatbotCategoryResponseDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.entities.MessageClass;
import com.example.demo.exceptions.ResourceConflictException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.ChatbotCategoryRepo;
import com.example.demo.repos.ChatbotRepo;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

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

// Coverage criteria: service unit coverage for chatbot category creation, listing, deletion, lookup,
// and category message-class delegation, including duplicate and not-found failures.
class ChatbotCategoryServiceTest {

    @Test
    void addCategorySavesNewCategory() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        ChatbotCategoryService service = service(categoryRepo, mock(MessageClassService.class), mock(ChatbotRepo.class));

        service.addCategory("Support");

        verify(categoryRepo).save(org.mockito.ArgumentMatchers.argThat(category ->
                "Support".equals(category.getName())));
    }

    @Test
    void addCategoryThrowsConflictWhenDatabaseRejectsDuplicate() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        ChatbotCategoryService service = service(categoryRepo, mock(MessageClassService.class), mock(ChatbotRepo.class));
        when(categoryRepo.save(org.mockito.ArgumentMatchers.any(ChatbotCategory.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> service.addCategory("Support"));

        assertEquals("Chatbot category name already exists with name: Support", exception.getMessage());
    }

    @Test
    void addCategoriesDelegatesEachName() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        ChatbotCategoryService service = service(categoryRepo, mock(MessageClassService.class), mock(ChatbotRepo.class));

        service.addCategories(List.of("Support", "Gaming"));

        verify(categoryRepo).save(org.mockito.ArgumentMatchers.argThat(category ->
                "Support".equals(category.getName())));
        verify(categoryRepo).save(org.mockito.ArgumentMatchers.argThat(category ->
                "Gaming".equals(category.getName())));
    }

    @Test
    void getAllCategoriesMapsCounts() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotCategoryService service = service(categoryRepo, mock(MessageClassService.class), chatbotRepo);
        ChatbotCategory support = ChatbotCategory.builder().id(1L).name("Support").build();
        when(categoryRepo.findByIsDeletedFalse()).thenReturn(List.of(support));
        when(chatbotRepo.countChatbotsGroupedByCategory()).thenReturn(List.<Object[]>of(new Object[]{1L, 4L}));

        List<ChatbotCategoryResponseDTO> categories = service.getAllCategories();

        assertEquals(1, categories.size());
        assertEquals(1L, categories.get(0).getId());
        assertEquals("Support", categories.get(0).getName());
        assertEquals(4, categories.get(0).getChatbotCount());
    }

    @Test
    void deleteCategorySoftDeletesWhenChatbotsUseCategory() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        MessageClassService messageClassService = mock(MessageClassService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotCategoryService service = service(categoryRepo, messageClassService, chatbotRepo);
        ChatbotCategory category = ChatbotCategory.builder().id(1L).name("Support").isDeleted(false).build();
        when(chatbotRepo.existsByCategoryId(1L)).thenReturn(true);
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

        service.deleteCategory(1L);

        assertTrue(category.isDeleted());
        verify(categoryRepo).save(category);
        verify(messageClassService).deleteAllMessageClassesByCategory(1L);
    }

    @Test
    void deleteCategoryHardDeletesWhenUnused() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        MessageClassService messageClassService = mock(MessageClassService.class);
        ChatbotRepo chatbotRepo = mock(ChatbotRepo.class);
        ChatbotCategoryService service = service(categoryRepo, messageClassService, chatbotRepo);
        when(chatbotRepo.existsByCategoryId(1L)).thenReturn(false);

        service.deleteCategory(1L);

        verify(messageClassService).deleteAllMessageClassesByCategory(1L);
        verify(categoryRepo).deleteById(1L);
    }

    @Test
    void getChabotCategoryByIdThrowsWhenMissing() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        ChatbotCategoryService service = service(categoryRepo, mock(MessageClassService.class), mock(ChatbotRepo.class));
        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getChabotCategoryById(99L));

        assertEquals("Chatbot category not found with id: 99", exception.getMessage());
    }

    @Test
    void createMessageClassForCategoryDelegatesAfterCategoryLookup() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        MessageClassService messageClassService = mock(MessageClassService.class);
        ChatbotCategoryService service = service(categoryRepo, messageClassService, mock(ChatbotRepo.class));
        ChatbotCategory category = ChatbotCategory.builder().id(1L).name("Support").build();
        List<String> names = List.of("Pricing");
        List<MessageClassResponseDTO> response = List.of(MessageClassResponseDTO.builder().id(10L).build());
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(messageClassService.createMessageClassForCategory(category, names)).thenReturn(response);

        List<MessageClassResponseDTO> result = service.createMessageClassForCategory(1L, names);

        assertSame(response, result);
    }

    @Test
    void deleteMessageClassForCategoryRemovesClassAndDelegatesDelete() {
        ChatbotCategoryRepo categoryRepo = mock(ChatbotCategoryRepo.class);
        MessageClassService messageClassService = mock(MessageClassService.class);
        ChatbotCategoryService service = service(categoryRepo, messageClassService, mock(ChatbotRepo.class));
        MessageClass messageClass = MessageClass.builder().id(5L).name("Pricing").build();
        ChatbotCategory category = ChatbotCategory.builder()
                .id(1L)
                .messageClasses(new HashSet<>(List.of(messageClass)))
                .build();
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

        service.deleteMessageClassForCategory(1L, 5L);

        assertFalse(category.getMessageClasses().contains(messageClass));
        verify(categoryRepo).save(category);
        verify(messageClassService).deleteMessageClassFromCategory(5L);
    }

    private static ChatbotCategoryService service(
            ChatbotCategoryRepo categoryRepo,
            MessageClassService messageClassService,
            ChatbotRepo chatbotRepo
    ) {
        ChatbotCategoryService service = new ChatbotCategoryService();
        ReflectionTestUtils.setField(service, "chatbotCategoryRepo", categoryRepo);
        ReflectionTestUtils.setField(service, "messageClassService", messageClassService);
        ReflectionTestUtils.setField(service, "chatbotRepo", chatbotRepo);
        return service;
    }
}
