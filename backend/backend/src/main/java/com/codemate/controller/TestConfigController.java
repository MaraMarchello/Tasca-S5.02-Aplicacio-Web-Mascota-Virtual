package com.codemate.controller;

import com.codemate.model.AIConversation;
import com.codemate.model.AIMessage;
import com.codemate.model.User;
import com.codemate.payload.response.AIConversationResponse;
import com.codemate.service.OpenAIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

    @GetMapping("/json-serialization")
    public ResponseEntity<Map<String, Object>> testJsonSerialization() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Create a test user
            User testUser = new User();
            testUser.setId(1L);
            testUser.setName("Test User");
            testUser.setEmail("test@example.com");
            
            // Create a test conversation
            AIConversation testConversation = new AIConversation();
            testConversation.setId(1L);
            testConversation.setUser(testUser);
            testConversation.setTitle("Test Conversation");
            testConversation.setContextType(AIConversation.ContextType.GENERAL);
            testConversation.setProgrammingLanguage("java");
            testConversation.setCreatedAt(LocalDateTime.now());
            testConversation.setUpdatedAt(LocalDateTime.now());
            testConversation.setIsActive(true);
            testConversation.setMessages(new ArrayList<>());
            
            // Create test messages
            AIMessage userMessage = AIMessage.createUserMessage("Hello, can you help me with Java?");
            userMessage.setId(1L);
            userMessage.setConversation(testConversation);
            userMessage.setCreatedAt(LocalDateTime.now());
            
            AIMessage assistantMessage = AIMessage.createAssistantMessage("Of course! I'd be happy to help you with Java programming.");
            assistantMessage.setId(2L);
            assistantMessage.setConversation(testConversation);
            assistantMessage.setCreatedAt(LocalDateTime.now());
            
            testConversation.getMessages().add(userMessage);
            testConversation.getMessages().add(assistantMessage);
            
            // Convert to DTO to test serialization
            AIConversationResponse conversationResponse = AIConversationResponse.from(testConversation);
            
            response.put("status", "success");
            response.put("message", "JSON serialization test completed successfully");
            response.put("conversationData", conversationResponse);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "JSON serialization failed: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return ResponseEntity.ok(response);
    }
} 