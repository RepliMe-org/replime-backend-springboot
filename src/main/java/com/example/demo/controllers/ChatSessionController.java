package com.example.demo.controllers;

import com.example.demo.services.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sessions")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;
}
