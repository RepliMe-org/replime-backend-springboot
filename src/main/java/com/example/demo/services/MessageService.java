package com.example.demo.services;

import com.example.demo.repos.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    @Autowired
    private MessageRepo messageRepo;

    // Mock implementation for existsByMessageClassId
    public boolean existsByMessageClassId(Long messageClassId) {
        return false;
    }
}

