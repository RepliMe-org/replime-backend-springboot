package com.example.demo.services;

import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Message;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.repos.MessageRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;
    @Autowired
    private MessageClassService messageClassService;

    public boolean existsByMessageClassId(Long messageClassId) {
        List<Message> messages = messageRepo.findByMessageClassId(messageClassId);
        return !messages.isEmpty();
    }

    public Message createMessage(ChatSession chatSession, String userMessage, MessageSender messageSender) {
        Message message = Message.builder()
                .content(userMessage)
                .session(chatSession)
                .sender(messageSender)
                .build();
        return messageRepo.save(message);
    }

    public void classifyMessage(Long messageId, Long messageClassId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message with id " + messageId + " not found"));
        MessageClass messageClass = messageClassService.getMessageClassById(messageClassId);
        message.setMessageClass(messageClass);
        messageRepo.save(message);
    }
}

