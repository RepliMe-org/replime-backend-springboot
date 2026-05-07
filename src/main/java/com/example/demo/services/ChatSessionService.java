package com.example.demo.services;

import com.example.demo.repos.ChatSessionRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatSessionService {

    @Autowired
    private ChatSessionRepo chatSessionRepo;
}
