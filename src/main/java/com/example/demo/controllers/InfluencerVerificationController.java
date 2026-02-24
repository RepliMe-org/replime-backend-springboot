package com.example.demo.controllers;

import com.example.demo.dtos.RequestVerificationDTO;
import com.example.demo.dtos.ResponseVerificationDTO;
import com.example.demo.services.InfluencerVerificationService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("influencer/verify")
public class InfluencerVerificationController {
    @Autowired
    private InfluencerVerificationService influencerVerificationService;

    @PostMapping("/request")
    public ResponseVerificationDTO requestVerification(@RequestBody RequestVerificationDTO requestVerificationDTO,
                                                       @RequestHeader("Authorization") String token) {
        ResponseVerificationDTO response = influencerVerificationService.requestVerification(
                requestVerificationDTO.getChannelUrl(), token);
        return response;
    }
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmVerification(@RequestHeader("Authorization") String token) {
        influencerVerificationService.confirmVerification(token);
        return ResponseEntity.ok("Influencer Verification Confirmed");
    }
}
