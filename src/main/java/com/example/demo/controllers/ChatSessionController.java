package com.example.demo.controllers;

import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.dtos.CreateSessionRequestDTO;
import com.example.demo.services.ChatSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @PostMapping
    public ResponseEntity<SessionResponseDTO> createSession(
            @RequestBody CreateSessionRequestDTO request,
            @RequestHeader("Authorization") String token){
        return ResponseEntity.ok(chatSessionService.createSession(request.getChatbotId(), token));
    }


}
