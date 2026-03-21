package com.example.demo.services;

import com.example.demo.dtos.ChatbotCategoryRequest;
import com.example.demo.entities.ChatbotCategory;
import com.example.demo.exceptions.ResourceConflictException;
import com.example.demo.repos.ChatbotCategoryRepo;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatbotCategoryServiceTest {

    @Test
    void addCategoryThrowsConflictWhenNameAlreadyExists() throws Exception {
        ChatbotCategoryService service = new ChatbotCategoryService();
        AtomicBoolean saveCalled = new AtomicBoolean(false);

        injectRepo(service, createRepoProxy(true, saveCalled, false, null));

        ChatbotCategoryRequest request = ChatbotCategoryRequest.builder()
                .name("Support")
                .build();

        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> service.addCategory(request));

        assertEquals("Chatbot category name already exists", ex.getMessage());
        assertFalse(saveCalled.get());
    }

    @Test
    void addCategoryThrowsConflictWhenDatabaseRejectsDuplicate() throws Exception {
        ChatbotCategoryService service = new ChatbotCategoryService();

        injectRepo(service, createRepoProxy(false, new AtomicBoolean(false), true, null));

        ChatbotCategoryRequest request = ChatbotCategoryRequest.builder()
                .name("Support")
                .build();

        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> service.addCategory(request));

        assertEquals("Chatbot category name already exists", ex.getMessage());
    }

    @Test
    void addCategorySavesWhenNameIsUnique() throws Exception {
        ChatbotCategoryService service = new ChatbotCategoryService();
        AtomicBoolean saveCalled = new AtomicBoolean(false);
        AtomicReference<ChatbotCategory> savedCategory = new AtomicReference<>();

        injectRepo(service, createRepoProxy(false, saveCalled, false, savedCategory));

        ChatbotCategoryRequest request = ChatbotCategoryRequest.builder()
                .name("Support")
                .build();

        service.addCategory(request);

        assertTrue(saveCalled.get());
        assertEquals("Support", savedCategory.get().getName());
    }

    private static void injectRepo(ChatbotCategoryService service, ChatbotCategoryRepo repo) throws Exception {
        Field repoField = ChatbotCategoryService.class.getDeclaredField("chatbotCategoryRepo");
        repoField.setAccessible(true);
        repoField.set(service, repo);
    }

    private static ChatbotCategoryRepo createRepoProxy(
            boolean categoryExists,
            AtomicBoolean saveCalled,
            boolean throwOnSave,
            AtomicReference<ChatbotCategory> savedCategory
    ) {
        return (ChatbotCategoryRepo) Proxy.newProxyInstance(
                ChatbotCategoryRepo.class.getClassLoader(),
                new Class[]{ChatbotCategoryRepo.class},
                (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "existsByName" -> categoryExists;
                        case "save" -> {
                            saveCalled.set(true);
                            if (throwOnSave) {
                                throw new DataIntegrityViolationException("duplicate key value violates unique constraint");
                            }
                            ChatbotCategory category = (ChatbotCategory) args[0];
                            if (savedCategory != null) {
                                savedCategory.set(category);
                            }
                            yield category;
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "ChatbotCategoryRepoProxy";
                        default -> defaultValue(method.getReturnType());
                    };
                }
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }

        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }

        return null;
    }
}

