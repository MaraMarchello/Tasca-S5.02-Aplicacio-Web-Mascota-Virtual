package com.codemate.controller;

import com.codemate.exception.AIServiceException;
import com.codemate.exception.BadRequestException;
import com.codemate.payload.request.AIAssistanceRequest;
import com.codemate.payload.response.AIAssistanceResponse;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.OpenAIService;
import com.codemate.service.PointAwardHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIAssistantController {

    private final OpenAIService openAIService;
    private final PointAwardHelper pointAwardHelper;

    @PostMapping("/code-assistance")
    public ResponseEntity<AIAssistanceResponse> getCodeAssistance(
            @Valid @RequestBody AIAssistanceRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Received code assistance request from user: {}", currentUser.getId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("Empty query received for code assistance");
            throw new BadRequestException("Query cannot be empty");
        }
        
        try {
            log.debug("Processing code assistance request: {}", request.getQuery());
            AIAssistanceResponse response = openAIService.getCodeAssistance(request.getQuery());
            
            // Award points for AI interaction
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "code-assistance");
            log.debug("Awarded AI chat points to user: {}", currentUser.getId());
            
            log.info("Code assistance request processed successfully");
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("Error processing code assistance request", e);
            throw e;
        }
    }

    @PostMapping("/explain-error")
    public ResponseEntity<AIAssistanceResponse> explainError(
            @Valid @RequestBody AIAssistanceRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Received error explanation request from user: {}", currentUser.getId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("Empty stack trace received for error explanation");
            throw new BadRequestException("Stack trace cannot be empty");
        }
        
        try {
            log.debug("Processing error explanation request");
            AIAssistanceResponse response = openAIService.explainError(request.getQuery());
            
            // Award points for stack trace resolution
            pointAwardHelper.awardStackTracePoints(currentUser.getId(), "error-explanation");
            log.debug("Awarded stack trace points to user: {}", currentUser.getId());
            
            log.info("Error explanation request processed successfully");
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("Error processing error explanation request", e);
            throw e;
        }
    }

    @PostMapping("/explain-git-error")
    public ResponseEntity<AIAssistanceResponse> explainGitError(
            @Valid @RequestBody AIAssistanceRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Received Git error explanation request from user: {}", currentUser.getId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("Empty Git error received for explanation");
            throw new BadRequestException("Git error message cannot be empty");
        }
        
        try {
            log.debug("Processing Git error explanation request");
            AIAssistanceResponse response = openAIService.explainGitError(request.getQuery());
            
            // Award points for problem solving (Git errors)
            pointAwardHelper.awardStackTracePoints(currentUser.getId(), "git-error-explanation");
            log.debug("Awarded problem solving points to user: {}", currentUser.getId());
            
            log.info("Git error explanation request processed successfully");
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("Error processing Git error explanation request", e);
            throw e;
        }
    }
} 