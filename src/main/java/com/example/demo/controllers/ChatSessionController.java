package com.example.demo.controllers;

import com.example.demo.dtos.SendMessageResponseDTO;
import com.example.demo.dtos.SessionListResponseDTO;
import com.example.demo.dtos.SessionResponseDTO;
import com.example.demo.dtos.CreateSessionRequestDTO;
import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.services.ChatSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/{sessionId}")
    @Operation(description = "Retrieve details of a specific chat session by its ID.")
    public ResponseEntity<SessionResponseDTO> getSessionDetails(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token
    ){
        return ResponseEntity.ok(chatSessionService.getSessionDetails(sessionId, token));
    }

    @GetMapping
    @Operation(description = "Get a paginated list of all chat sessions for a specific chatbot." +
            " Requires the chatbot ID as a query parameter and an authorization token in the request header. Supports pagination through cursor and limit parameters.")
    public ResponseEntity<SessionListResponseDTO> getAllSessions(
            @RequestHeader("Authorization") String token,
            @RequestParam UUID chatbotId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(chatSessionService.getAllSessions(
                token, chatbotId, cursor, limit
        ));
    }

    @PostMapping("/{sessionId}/messages")
    @Operation(description = "Send message to specific chat session")
    public ResponseEntity<SendMessageResponseDTO> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody String userMessage,
            @RequestHeader("Authorization") String token
    ){
        return ResponseEntity.ok(chatSessionService.sendMessage(sessionId, userMessage, token));
    }

    @GetMapping("/{sessionId}/messages")
    @Operation(description = "Get messages of a specific chat session")
    public ResponseEntity<List<MessageDto>> getSessionMessages(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String token
    ){
        return ResponseEntity.ok(chatSessionService.getSessionMessages(sessionId, token));
    }
}
