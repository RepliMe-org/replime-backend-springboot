package com.example.demo.services;

import com.example.demo.entities.Message;
import com.example.demo.repos.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;

    public boolean existsByMessageClassId(Long messageClassId) {
        List<Message> messages = messageRepo.findByMessageClassId(messageClassId);
        return !messages.isEmpty();
    }
}

