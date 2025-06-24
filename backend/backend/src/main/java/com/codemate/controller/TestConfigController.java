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
    private final String openaiModel;
    private final OpenAIService openAIService;

    public TestConfigController(
            @Value("${openai.api.key}") String openaiApiKey,
            @Value("${openai.api.model:gpt-3.5-turbo}") String openaiModel,
            OpenAIService openAIService) {
        this.openaiApiKey = openaiApiKey;
        this.openaiModel = openaiModel;
        this.openAIService = openAIService;
    }

    @GetMapping("/config")
    public ResponseEntity<String> testConfig() {
        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            return ResponseEntity.badRequest().body("OpenAI API key is not set!");
        }
        
        // Only show the first few characters for security
        String maskedKey = openaiApiKey.substring(0, Math.min(5, openaiApiKey.length())) + "..." + 
                          openaiApiKey.substring(Math.max(0, openaiApiKey.length() - 5));
        
        return ResponseEntity.ok(String.format(
            "OpenAI Configuration:\n" +
            "- API Key: %s\n" +
            "- Model: %s\n" +
            "- Status: Ready", 
            maskedKey, openaiModel));
    }

    @GetMapping("/connection")
    public ResponseEntity<String> testConnection() {
        return ResponseEntity.ok(openAIService.testConnection());
    }
} 