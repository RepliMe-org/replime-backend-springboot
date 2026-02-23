package com.example.demo.controllers;


import com.example.demo.configs.JwtService;
import com.example.demo.dtos.RequestVerificationDTO;
import com.example.demo.entities.User;
import com.example.demo.repos.UserRepo;
import com.example.demo.services.InfluencerVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("influencer/verify")
public class InfluencerVerificationController {
    @Autowired
    private InfluencerVerificationService influencerVerificationService;

    @PostMapping("/request")
    public String requestVerification(@RequestBody RequestVerificationDTO requestVerificationDTO, @RequestHeader("Authorization") String token) {
        String verificationToken = influencerVerificationService.requestVerification(
                requestVerificationDTO.getChannelUrl(), token);
        return verificationToken;
    }
}
