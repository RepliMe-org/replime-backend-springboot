package com.example.demo.controllers;

import com.example.demo.repos.VideoRepository;
import com.example.demo.services.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal")
public class InternalController {
    @Autowired
    private VideoService videoService;
    @PatchMapping("/update-video-status/{videoId}")
    public ResponseEntity<String> updateVideoStatus(
            @PathVariable String videoId, @RequestParam("status") String status) {
        videoService.updateVideoStatus(videoId,status);
        return ResponseEntity.ok("Video status updated successfully");
    }
}
