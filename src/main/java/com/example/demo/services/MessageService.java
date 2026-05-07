package com.example.demo.services;

import org.springframework.stereotype.Service;

@Service
public class MessageService {

    // Mock implementation for existsByMessageClassId
    public boolean existsByMessageClassId(Long messageClassId) {
        return false;
    }
}

