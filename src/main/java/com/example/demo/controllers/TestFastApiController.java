package com.example.demo.controllers;

import com.example.demo.services.FastApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class TestFastApiController {
    @Autowired
    private FastApiService fastApiService;


    @GetMapping("/test-fastapi")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = fastApiService.callFastApi();
        System.out.println(response.get("message"));
        return ResponseEntity.ok(response);
    }
}
