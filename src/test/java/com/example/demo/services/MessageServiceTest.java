package com.example.demo.services;

import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Chatbot;
import com.example.demo.entities.Message;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.exceptions.InvalidClassificationException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.MessageClassRepo;
import com.example.demo.repos.MessageRepo;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Coverage criteria: service unit coverage for message persistence helpers and classification rules,
// verifying already-classified, non-user, missing class, foreign class, not-found, and success paths.
class MessageServiceTest {

    @Test
    void existsByMessageClassIdReturnsTrueWhenMessagesExist() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageService service = service(messageRepo, mock(MessageClassRepo.class));
        when(messageRepo.findByMessageClassId(3L)).thenReturn(List.of(Message.builder().id(1L).build()));

        assertTrue(service.existsByMessageClassId(3L));
    }

    @Test
    void createMessageBuildsAndFlushesMessage() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageService service = service(messageRepo, mock(MessageClassRepo.class));
        ChatSession session = ChatSession.builder().id(9L).build();
        when(messageRepo.saveAndFlush(org.mockito.ArgumentMatchers.any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Message message = service.createMessage(session, "hello", MessageSender.USER);

        assertSame(session, message.getSession());
        assertEquals("hello", message.getContent());
        assertEquals(MessageSender.USER, message.getSender());
    }

    @Test
    void getMessageThrowsWhenMissing() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageService service = service(messageRepo, mock(MessageClassRepo.class));
        when(messageRepo.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> service.getMessage(99L));

        assertEquals("Message with id 99 not found", exception.getMessage());
    }

    @Test
    void classifyMessageAssignsAllowedClassToUserMessage() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageClassRepo messageClassRepo = mock(MessageClassRepo.class);
        MessageService service = service(messageRepo, messageClassRepo);
        MessageClass messageClass = MessageClass.builder().id(4L).name("Pricing").build();
        Chatbot chatbot = Chatbot.builder().messageClasses(new HashSet<>(List.of(messageClass))).build();
        ChatSession session = ChatSession.builder().chatbot(chatbot).build();
        Message message = Message.builder()
                .id(11L)
                .session(session)
                .content("How much?")
                .sender(MessageSender.USER)
                .build();
        when(messageRepo.findById(11L)).thenReturn(Optional.of(message));
        when(messageClassRepo.findById(4L)).thenReturn(Optional.of(messageClass));

        MessageDto response = service.classifyMessage(11L, 4L);

        assertSame(messageClass, message.getMessageClass());
        assertEquals(11L, response.getId());
        assertEquals("How much?", response.getMessage());
        assertEquals("Pricing", response.getMessageClass());
        verify(messageRepo).save(message);
    }

    @Test
    void classifyMessageThrowsWhenAlreadyClassified() {
        MessageService service = service(mock(MessageRepo.class), mock(MessageClassRepo.class));
        Message message = Message.builder()
                .id(11L)
                .messageClass(MessageClass.builder().id(1L).build())
                .sender(MessageSender.USER)
                .build();
        MessageRepo messageRepo = (MessageRepo) ReflectionTestUtils.getField(service, "messageRepo");
        when(messageRepo.findById(11L)).thenReturn(Optional.of(message));

        InvalidClassificationException exception = assertThrows(
                InvalidClassificationException.class,
                () -> service.classifyMessage(11L, 4L));

        assertEquals("Message is already classified", exception.getMessage());
    }

    @Test
    void classifyMessageThrowsForBotMessage() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageService service = service(messageRepo, mock(MessageClassRepo.class));
        Message message = Message.builder().id(11L).sender(MessageSender.BOT).build();
        when(messageRepo.findById(11L)).thenReturn(Optional.of(message));

        InvalidClassificationException exception = assertThrows(
                InvalidClassificationException.class,
                () -> service.classifyMessage(11L, 4L));

        assertEquals("Only user messages can be classified", exception.getMessage());
    }

    @Test
    void classifyMessageThrowsWhenMessageClassMissing() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageClassRepo messageClassRepo = mock(MessageClassRepo.class);
        MessageService service = service(messageRepo, messageClassRepo);
        Message message = Message.builder().id(11L).sender(MessageSender.USER).build();
        when(messageRepo.findById(11L)).thenReturn(Optional.of(message));
        when(messageClassRepo.findById(4L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.classifyMessage(11L, 4L));

        assertEquals("Message class not found with id: 4", exception.getMessage());
    }

    @Test
    void classifyMessageThrowsWhenClassDoesNotBelongToChatbot() {
        MessageRepo messageRepo = mock(MessageRepo.class);
        MessageClassRepo messageClassRepo = mock(MessageClassRepo.class);
        MessageService service = service(messageRepo, messageClassRepo);
        MessageClass messageClass = MessageClass.builder().id(4L).name("Pricing").build();
        ChatSession session = ChatSession.builder()
                .chatbot(Chatbot.builder().messageClasses(new HashSet<>()).build())
                .build();
        Message message = Message.builder().id(11L).session(session).sender(MessageSender.USER).build();
        when(messageRepo.findById(11L)).thenReturn(Optional.of(message));
        when(messageClassRepo.findById(4L)).thenReturn(Optional.of(messageClass));

        InvalidClassificationException exception = assertThrows(
                InvalidClassificationException.class,
                () -> service.classifyMessage(11L, 4L));

        assertEquals("Message class does not belong to the chatbot handling this session", exception.getMessage());
    }

    private static MessageService service(MessageRepo messageRepo, MessageClassRepo messageClassRepo) {
        MessageService service = new MessageService();
        ReflectionTestUtils.setField(service, "messageRepo", messageRepo);
        ReflectionTestUtils.setField(service, "messageClassRepo", messageClassRepo);
        return service;
    }
}
