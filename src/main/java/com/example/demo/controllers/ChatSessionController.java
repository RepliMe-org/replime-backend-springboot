package com.example.demo.controllers;

import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.dtos.CreateSessionRequestDTO;
import com.example.demo.services.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@Tag(name = "chat sessions",description = "Endpoints for managing chat sessions. Allows users to create and manage their chat sessions with chatbots.")
public class ChatSessionController {

    @Autowired
    private ChatSessionService chatSessionService;

    @PostMapping
    @Operation(description = "Create a new chat session with a specified chatbot. Requires the chatbot ID and an authorization token in the request header.")
    public ResponseEntity<SessionResponseDTO> createSession(
            @RequestBody CreateSessionRequestDTO request,
            @RequestHeader("Authorization") String token){
        return ResponseEntity.ok(chatSessionService.createSession(request.getChatbotId(), token));
    }

//    @GetMapping("/sessionId")
//    public ResponseEntity<SessionResponseDTO> getSessionDetails(
//            @PathVariable Long sessionId,
//            @RequestHeader("Authorization") String token
//    ){
//        return ResponseEntity.ok(chatSessionService.getSessionDetails(sessionId, token));
//    }
}
