package com.example.demo.controllers;

import com.example.demo.dtos.internal.UpdateAiDescriptionRequestDTO;
import com.example.demo.dtos.internal.UpdateVideoStatusRequestDTO;
import com.example.demo.dtos.utils.MessageDto;
import com.example.demo.services.ChatbotConfigService;
import com.example.demo.services.MessageService;
import com.example.demo.services.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalController {
    @Autowired
    private VideoService videoService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ChatbotConfigService chatbotConfigService;

    @PatchMapping("/update-video-status/{videoId}")
    public ResponseEntity<String> updateVideoStatus(
            @PathVariable String videoId, @RequestBody UpdateVideoStatusRequestDTO request) {
        System.out.println(" ingest video error: "+request.getFailureReason());
        videoService.updateVideoStatus(videoId,request);
        return ResponseEntity.ok("Video status updated successfully");
    }

    @PatchMapping("/chatbots/{chatbotId}/ai-description")
    public ResponseEntity<String> updateAiDescription(
            @PathVariable UUID chatbotId,
            @RequestBody UpdateAiDescriptionRequestDTO request) {
        chatbotConfigService.updateAiGeneratedDescription(chatbotId, request.getDescription());
        return ResponseEntity.ok("AI description updated successfully");
    }

    @GetMapping("/ws-test")
    public void testWs() {
        messagingTemplate.convertAndSend("/topic/test", "HELLO");
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<MessageDto> updateMessageClasses(
            @PathVariable Long messageId,
            @RequestBody Long messageClassId
    ){

        return ResponseEntity.ok(messageService.classifyMessage(messageId,messageClassId));
    }
}
