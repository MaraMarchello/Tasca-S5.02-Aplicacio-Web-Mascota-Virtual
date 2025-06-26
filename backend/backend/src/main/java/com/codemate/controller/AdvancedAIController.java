package com.codemate.controller;

import com.codemate.exception.AIServiceException;
import com.codemate.exception.BadRequestException;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.AdvancedAIService;
import com.codemate.service.PointAwardHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai/advanced")
@RequiredArgsConstructor
public class AdvancedAIController {

    private final AdvancedAIService advancedAIService;
    private final PointAwardHelper pointAwardHelper;

    @PostMapping("/analyze-code")
    public ResponseEntity<AdvancedAIService.CodeAnalysisResult> analyzeCode(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Code analysis request from user: {}", currentUser.getId());
        
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");
        
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Code cannot be empty");
        }
        
        try {
            AdvancedAIService.CodeAnalysisResult result = advancedAIService.analyzeCode(
                    code, language, currentUser.getId());
            
            // Award points for code analysis
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "code-analysis");
            
            log.info("Code analysis completed for user: {}", currentUser.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error analyzing code for user: {}", currentUser.getId(), e);
            throw new AIServiceException("Failed to analyze code: " + e.getMessage(), e);
        }
    }

    @PostMapping("/generate-code")
    public ResponseEntity<AdvancedAIService.CodeGenerationResult> generateCode(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Code generation request from user: {}", currentUser.getId());
        
        String requirements = request.get("requirements");
        String language = request.getOrDefault("language", "java");
        String context = request.get("context");
        
        if (requirements == null || requirements.trim().isEmpty()) {
            throw new BadRequestException("Requirements cannot be empty");
        }
        
        try {
            AdvancedAIService.CodeGenerationResult result = advancedAIService.generateCode(
                    requirements, language, context, currentUser.getId());
            
            // Award points for code generation
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "code-generation");
            
            log.info("Code generation completed for user: {}", currentUser.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error generating code for user: {}", currentUser.getId(), e);
            throw new AIServiceException("Failed to generate code: " + e.getMessage(), e);
        }
    }

    @PostMapping("/code-suggestions")
    public ResponseEntity<List<AdvancedAIService.CodeSuggestion>> getCodeSuggestions(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Code suggestions request from user: {}", currentUser.getId());
        
        String partialCode = request.get("partialCode");
        String language = request.getOrDefault("language", "java");
        String context = request.get("context");
        
        if (partialCode == null || partialCode.trim().isEmpty()) {
            throw new BadRequestException("Partial code cannot be empty");
        }
        
        try {
            List<AdvancedAIService.CodeSuggestion> suggestions = advancedAIService.getCodeSuggestions(
                    partialCode, language, context, currentUser.getId());
            
            // Award points for using suggestions
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "code-suggestions");
            
            log.info("Code suggestions completed for user: {}", currentUser.getId());
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error getting code suggestions for user: {}", currentUser.getId(), e);
            throw new AIServiceException("Failed to get code suggestions: " + e.getMessage(), e);
        }
    }

    @PostMapping("/analyze-complexity")
    public ResponseEntity<AdvancedAIService.CodeComplexityAnalysis> analyzeComplexity(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Complexity analysis request from user: {}", currentUser.getId());
        
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");
        
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Code cannot be empty");
        }
        
        try {
            AdvancedAIService.CodeComplexityAnalysis analysis = advancedAIService.analyzeComplexity(
                    code, language, currentUser.getId());
            
            // Award points for complexity analysis
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "complexity-analysis");
            
            log.info("Complexity analysis completed for user: {}", currentUser.getId());
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("Error analyzing complexity for user: {}", currentUser.getId(), e);
            throw new AIServiceException("Failed to analyze complexity: " + e.getMessage(), e);
        }
    }

    @PostMapping("/generate-tests")
    public ResponseEntity<AdvancedAIService.TestGenerationResult> generateTests(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Test generation request from user: {}", currentUser.getId());
        
        String code = request.get("code");
        String language = request.getOrDefault("language", "java");
        String testFramework = request.getOrDefault("testFramework", "JUnit");
        
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Code cannot be empty");
        }
        
        try {
            AdvancedAIService.TestGenerationResult result = advancedAIService.generateTests(
                    code, language, testFramework, currentUser.getId());
            
            // Award points for test generation
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "test-generation");
            
            log.info("Test generation completed for user: {}", currentUser.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error generating tests for user: {}", currentUser.getId(), e);
            throw new AIServiceException("Failed to generate tests: " + e.getMessage(), e);
        }
    }
} 