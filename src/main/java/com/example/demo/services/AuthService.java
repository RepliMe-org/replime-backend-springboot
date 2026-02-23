package com.example.demo.services;

import com.example.demo.dtos.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.configs.JwtService;
import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.dtos.SignupRequestDTO;
import com.example.demo.entities.AuthProvider;
import com.example.demo.entities.Role;
import com.example.demo.entities.User;
import com.example.demo.repos.UserRepo;

@Service
public class AuthService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public LoginResponseDTO signup(SignupRequestDTO request) {

        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();

        userRepo.save(user);
        String token = jwtService.generateToken(user);
        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token(token)
                .username(user.getName())
                .role(user.getRole())
                .build();

        return loginResponse;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials!"));

        String token = jwtService.generateToken(user);

        LoginResponseDTO loginResponse = LoginResponseDTO.builder()
                .token(token)
                .username(user.getName())
                .role(user.getRole())
                .build();

        return loginResponse;
    }
}