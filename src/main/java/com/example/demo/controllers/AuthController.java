package com.example.demo.controllers;

import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.dtos.LoginResponseDTO;
import com.example.demo.dtos.SignupRequestDTO;
import com.example.demo.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("signup")
    public ResponseEntity<LoginResponseDTO> signup(@RequestBody SignupRequestDTO signupRequest) {
        LoginResponseDTO loginResponse = authService.signup(signupRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request){
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("loggedin")
    public String loggedin(){
        return "successfully logged in";
    }

    // http://localhost:8080/api/v1/login/oauth2/code/google
}