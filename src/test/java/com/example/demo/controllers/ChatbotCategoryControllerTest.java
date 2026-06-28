package com.example.demo.controllers;

import com.example.demo.dtos.ChatbotCategoryResponseDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.services.ChatbotCategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// Coverage criteria: direct controller unit coverage for category CRUD and category message-class endpoints,
// verifying service delegation, path/body forwarding, and fixed success messages.
class ChatbotCategoryControllerTest {

    @Test
    void addChatbotCategoryPassesNamesAndReturnsSuccessMessage() throws Exception {
        ChatbotCategoryController controller = new ChatbotCategoryController();
        TestChatbotCategoryService service = new TestChatbotCategoryService();
        injectService(controller, service);
        List<String> names = List.of("Support", "Gaming");

        ResponseEntity<String> response = controller.addChatbotCategory(names);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Chatbot category added successfully", response.getBody());
        assertSame(names, service.addedNames.get());
    }

    @Test
    void getAllChatbotCategoriesReturnsServiceCategories() throws Exception {
        ChatbotCategoryController controller = new ChatbotCategoryController();
        TestChatbotCategoryService service = new TestChatbotCategoryService();
        service.categories = List.of(ChatbotCategoryResponseDTO.builder().id(1L).name("Support").build());
        injectService(controller, service);

        ResponseEntity<List<ChatbotCategoryResponseDTO>> response = controller.getAllChatbotCategories();

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.categories, response.getBody());
    }

    @Test
    void deleteChatbotCategoryPassesIdAndReturnsSuccessMessage() throws Exception {
        ChatbotCategoryController controller = new ChatbotCategoryController();
        TestChatbotCategoryService service = new TestChatbotCategoryService();
        injectService(controller, service);

        ResponseEntity<String> response = controller.deleteChatbotCategory(7L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Chatbot category deleted successfully", response.getBody());
        assertEquals(7L, service.deletedCategoryId.get());
    }

    @Test
    void getAllCategoryMessageClassesForAdminPassesCategoryId() throws Exception {
        ChatbotCategoryController controller = new ChatbotCategoryController();
        TestChatbotCategoryService service = new TestChatbotCategoryService();
        service.messageClasses = List.of(MessageClassResponseDTO.builder().id(3L).name("Pricing").build());
        injectService(controller, service);

        ResponseEntity<List<MessageClassResponseDTO>> response =
                controller.GetAllCategoryMessageClassesForAdmin(4L);

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.messageClasses, response.getBody());
        assertEquals(4L, service.classesCategoryId.get());
    }

    @Test
    void createMessageClassForAdminPassesCategoryIdAndNames() throws Exception {
        ChatbotCategoryController controller = new ChatbotCategoryController();
        TestChatbotCategoryService service = new TestChatbotCategoryService();
        service.messageClasses = List.of(MessageClassResponseDTO.builder().id(3L).name("Pricing").build());
        injectService(controller, service);
        List<String> names = List.of("Pricing");

        ResponseEntity<List<MessageClassResponseDTO>> response =
                controller.CreateMessageClassForAdmin(4L, names);

        assertEquals(200, response.getStatusCode().value());
        assertSame(service.messageClasses, response.getBody());
        assertEquals(4L, service.createdClassesCategoryId.get());
        assertSame(names, service.createdClassNames.get());
    }

    @Test
    void deleteMessageClassForAdminPassesIdsAndReturnsSuccessMessage() throws Exception {
        ChatbotCategoryController controller = new ChatbotCategoryController();
        TestChatbotCategoryService service = new TestChatbotCategoryService();
        injectService(controller, service);

        ResponseEntity<String> response = controller.DeleteMessageClassForAdmin(4L, 9L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Message class got deleted for a specific category.", response.getBody());
        assertEquals(4L, service.deletedClassCategoryId.get());
        assertEquals(9L, service.deletedClassId.get());
    }

    private static void injectService(ChatbotCategoryController controller, ChatbotCategoryService service) throws Exception {
        Field field = ChatbotCategoryController.class.getDeclaredField("chatbotCategoryService");
        field.setAccessible(true);
        field.set(controller, service);
    }

    private static class TestChatbotCategoryService extends ChatbotCategoryService {
        private List<ChatbotCategoryResponseDTO> categories;
        private List<MessageClassResponseDTO> messageClasses;
        private final AtomicReference<List<String>> addedNames = new AtomicReference<>();
        private final AtomicReference<Long> deletedCategoryId = new AtomicReference<>();
        private final AtomicReference<Long> classesCategoryId = new AtomicReference<>();
        private final AtomicReference<Long> createdClassesCategoryId = new AtomicReference<>();
        private final AtomicReference<List<String>> createdClassNames = new AtomicReference<>();
        private final AtomicReference<Long> deletedClassCategoryId = new AtomicReference<>();
        private final AtomicReference<Long> deletedClassId = new AtomicReference<>();

        @Override
        public void addCategories(List<String> chatbotCategoryNames) {
            addedNames.set(chatbotCategoryNames);
        }

        @Override
        public List<ChatbotCategoryResponseDTO> getAllCategories() {
            return categories;
        }

        @Override
        public void deleteCategory(Long id) {
            deletedCategoryId.set(id);
        }

        @Override
        public List<MessageClassResponseDTO> getAllClassesInCategory(Long categoryId) {
            classesCategoryId.set(categoryId);
            return messageClasses;
        }

        @Override
        public List<MessageClassResponseDTO> createMessageClassForCategory(
                Long categoryId,
                List<String> messageClassesNames
        ) {
            createdClassesCategoryId.set(categoryId);
            createdClassNames.set(messageClassesNames);
            return messageClasses;
        }

        @Override
        public void deleteMessageClassForCategory(Long categoryId, Long classId) {
            deletedClassCategoryId.set(categoryId);
            deletedClassId.set(classId);
        }
    }
}
