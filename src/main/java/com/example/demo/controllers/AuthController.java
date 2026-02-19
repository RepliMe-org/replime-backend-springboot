package com.example.demo.controllers;

import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.dtos.SignupRequestDTO;
import com.example.demo.services.AuthService;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequestDTO signupRequest) {
        String token = authService.signup(signupRequest);
        return ResponseEntity.ok(token);
    }

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request){
        return ResponseEntity.ok(authService.login(request));
    }

}