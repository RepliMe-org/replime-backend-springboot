package com.example.demo.services;

import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.entities.ChatSession;
import com.example.demo.entities.Message;
import com.example.demo.entities.MessageClass;
import com.example.demo.entities.utils.MessageSender;
import com.example.demo.exceptions.InvalidClassificationException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repos.MessageClassRepo;
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
    private MessageClassRepo messageClassRepo;

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

    public MessageDto classifyMessage(Long messageId, Long messageClassId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message with id " + messageId + " not found"));
        if (message.getMessageClass() != null) {
            throw new InvalidClassificationException("Message is already classified");
        }

        if (message.getSender() != MessageSender.USER) {
            throw new InvalidClassificationException("Only user messages can be classified");
        }

        MessageClass messageClass = messageClassRepo.findById(messageClassId)
                .orElseThrow(() -> new ResourceNotFoundException("Message class not found with id: " + messageClassId));
        
        if (!message.getSession().getChatbot().getMessageClasses().contains(messageClass)) {
            throw new InvalidClassificationException("Message class does not belong to the chatbot handling this session");
        }

        message.setMessageClass(messageClass);
        messageRepo.save(message);

        return MessageDto.builder()
                .id(message.getId())
                .message(message.getContent())
                .sender(message.getSender())
                .sentAt(message.getSentAt())
                .messageStatus(message.getStatus())
                .messageClass(messageClass.getName())
                .build();
    }
}
