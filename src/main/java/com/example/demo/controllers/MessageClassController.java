package com.example.demo.controllers;

import com.example.demo.dtos.MessageClassRequestDTO;
import com.example.demo.dtos.MessageClassResponseDTO;
import com.example.demo.services.MessageClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message-class")
public class MessageClassController {
    @Autowired
    private MessageClassService messageClassService;


//    this to show category classes for admin
    @GetMapping("{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MessageClassResponseDTO>> GetAllCategoryMessageClassesForAdmin(
            @PathVariable Long categoryId
    ) {
        return ResponseEntity.ok(
                messageClassService.getAllMainMessageClasses(categoryId));
    }

    //    this to show the message classes for influencer's chatbot category
    @GetMapping()
    @PreAuthorize("hasRole( 'INFLUENCER')")
    public ResponseEntity<List<MessageClassResponseDTO>> GetAllCategoryMessageClassesForInfluencer(
            @RequestHeader("Authorization") String token
    ) {
        return ResponseEntity.ok(
                messageClassService.getAllMessageClassesByCategoryUserToken(token));
    }
    //TODO: make the api take list of message classes in the body
    @PostMapping("{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MessageClassResponseDTO>> CreateMessageClassForAdmin(
            @PathVariable Long categoryId,
            @RequestBody MessageClassRequestDTO messageClassRequestDTO
    ){
        return ResponseEntity.ok(
                messageClassService.createMessageClassForCategory(
                        categoryId,messageClassRequestDTO));
    }
    //TODO: make the api take list of message classes in the body
    @PostMapping()
    @PreAuthorize("hasRole('INFLUENCER')")
    public ResponseEntity<MessageClassResponseDTO> CreateMessageClassForInfluencer(
            @RequestBody MessageClassRequestDTO messageClassRequestDTO,
            @RequestHeader("Authorization") String token
    ){
        return ResponseEntity.ok(
                messageClassService.createMessageClassForSpecificChatbot(
                        token,messageClassRequestDTO));
    }
    //TODO: make influencer choose classes that he wants from the system classes

    //TODO: add update and delete for message class, but only for influencer,
    // admin can only create the system classes and influencer
    // can choose from them and create custom classes, but cannot update or delete system classes
}
