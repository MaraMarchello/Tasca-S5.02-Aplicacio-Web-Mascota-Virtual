package com.codemate.controller;

import com.codemate.service.OpenAIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
public class TestConfigController {

    private final String openaiApiKey;
    private final OpenAIService openAIService;

    public TestConfigController(
            @Value("${openai.api.key}") String openaiApiKey,
            OpenAIService openAIService) {
        this.openaiApiKey = openaiApiKey;
        this.openAIService = openAIService;
    }

    @GetMapping("/config")
    public ResponseEntity<String> testConfig() {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return ResponseEntity.badRequest().body("OpenAI API key is not set!");
        }
        
        // Only show the first few characters for security
        String maskedKey = openaiApiKey.substring(0, 5) + "..." + 
                          openaiApiKey.substring(openaiApiKey.length() - 5);
        return ResponseEntity.ok("OpenAI API key is configured (starts with: " + maskedKey + ")");
    }

    @GetMapping("/connection")
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok(openAIService.testConnection());
    }
} 