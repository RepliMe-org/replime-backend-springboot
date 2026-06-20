package com.example.demo.controllers;

import com.example.demo.dtos.UserInfoResponseDTO;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    @Autowired
    private  UserService userService;

    @GetMapping
    public List<UserInfoResponseDTO> getUsers(
    ){
        return userService.getAllUsers();
    }

    @PatchMapping("/{email}/promote")
    public ResponseEntity<String> promoteToAdmin(@PathVariable String email) {
        userService.promoteToAdmin(email);
        return ResponseEntity.ok("User promoted to admin successfully");

    }

}
