package com.codemate.controller;

import com.codemate.exception.AIServiceException;
import com.codemate.exception.BadRequestException;
import com.codemate.model.AIConversation;
import com.codemate.model.AIMessage;
import com.codemate.payload.request.AIAssistanceRequest;
import com.codemate.payload.response.AIAssistanceResponse;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.AIConversationService;
import com.codemate.service.CodeExecutionService;
import com.codemate.service.OpenAIService;
import com.codemate.service.PointAwardHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIAssistantController {

    private final OpenAIService openAIService;
    private final AIConversationService conversationService;
    private final CodeExecutionService codeExecutionService;
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

    // NEW CONVERSATION-BASED ENDPOINTS

    @PostMapping("/chat")
    public ResponseEntity<AIAssistanceResponse> chatWithMemory(
            @Valid @RequestBody AIAssistanceRequest request,
            @RequestParam(defaultValue = "general") String contextType,
            @RequestParam(defaultValue = "java") String language,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Received chat request with memory from user: {}", currentUser.getId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("Empty query received for chat");
            throw new BadRequestException("Query cannot be empty");
        }
        
        try {
            AIConversation.ContextType context = AIConversation.ContextType.fromString(contextType);
            
            // Build context data
            Map<String, Object> contextData = new HashMap<>();
            if (request.getCodeSnippet() != null) {
                contextData.put("hasCode", true);
                contextData.put("codeLength", request.getCodeSnippet().length());
            }
            contextData.put("requestTime", System.currentTimeMillis());
            
            AIAssistanceResponse response = openAIService.getAssistanceWithMemory(
                    currentUser.getId(), request.getQuery(), context, language,
                    request.getCodeSnippet(), contextData);
            
            // Award points for AI interaction
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "chat-with-memory");
            log.debug("Awarded AI chat points to user: {}", currentUser.getId());
            
            log.info("Chat request with memory processed successfully");
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("Error processing chat request with memory", e);
            throw e;
        }
    }

    @PostMapping("/conversations/{conversationId}/continue")
    public ResponseEntity<AIAssistanceResponse> continueConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody AIAssistanceRequest request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Continuing conversation: {} for user: {}", conversationId, currentUser.getId());
        
        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            log.warn("Empty query received for conversation continuation");
            throw new BadRequestException("Query cannot be empty");
        }
        
        try {
            Map<String, Object> contextData = new HashMap<>();
            if (request.getCodeSnippet() != null) {
                contextData.put("hasCode", true);
                contextData.put("codeLength", request.getCodeSnippet().length());
            }
            contextData.put("requestTime", System.currentTimeMillis());
            
            AIAssistanceResponse response = openAIService.continueConversation(
                    conversationId, currentUser.getId(), request.getQuery(),
                    request.getCodeSnippet(), contextData);
            
            // Award points for AI interaction
            pointAwardHelper.awardAIChatPoints(currentUser.getId(), "continue-conversation");
            log.debug("Awarded AI chat points to user: {}", currentUser.getId());
            
            log.info("Conversation continuation processed successfully");
            return ResponseEntity.ok(response);
        } catch (AIServiceException e) {
            log.error("Error continuing conversation: {}", conversationId, e);
            throw e;
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<Page<AIConversation>> getUserConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting conversations for user: {}", currentUser.getId());
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<AIConversation> conversations = conversationService.getUserConversations(
                    currentUser.getId(), pageable);
            
            log.debug("Retrieved {} conversations for user: {}", 
                     conversations.getTotalElements(), currentUser.getId());
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Error getting user conversations", e);
            throw new AIServiceException("Failed to retrieve conversations: " + e.getMessage(), e);
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<AIConversation> getConversation(
            @PathVariable Long conversationId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting conversation: {} for user: {}", conversationId, currentUser.getId());
        
        try {
            AIConversation conversation = conversationService.getConversation(
                    conversationId, currentUser.getId());
            
            log.debug("Retrieved conversation: {}", conversationId);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            log.error("Error getting conversation: {}", conversationId, e);
            throw new AIServiceException("Failed to retrieve conversation: " + e.getMessage(), e);
        }
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<AIMessage>> getConversationMessages(
            @PathVariable Long conversationId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting messages for conversation: {}", conversationId);
        
        try {
            List<AIMessage> messages = conversationService.getConversationMessages(
                    conversationId, currentUser.getId());
            
            log.debug("Retrieved {} messages for conversation: {}", 
                     messages.size(), conversationId);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting conversation messages: {}", conversationId, e);
            throw new AIServiceException("Failed to retrieve messages: " + e.getMessage(), e);
        }
    }

    @PutMapping("/conversations/{conversationId}/archive")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable Long conversationId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Archiving conversation: {} for user: {}", conversationId, currentUser.getId());
        
        try {
            conversationService.archiveConversation(conversationId, currentUser.getId());
            
            log.info("Archived conversation: {}", conversationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error archiving conversation: {}", conversationId, e);
            throw new AIServiceException("Failed to archive conversation: " + e.getMessage(), e);
        }
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Deleting conversation: {} for user: {}", conversationId, currentUser.getId());
        
        try {
            conversationService.deleteConversation(conversationId, currentUser.getId());
            
            log.info("Deleted conversation: {}", conversationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting conversation: {}", conversationId, e);
            throw new AIServiceException("Failed to delete conversation: " + e.getMessage(), e);
        }
    }

    @GetMapping("/conversations/search")
    public ResponseEntity<List<AIConversation>> searchConversations(
            @RequestParam String query,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Searching conversations for user: {} with query: {}", currentUser.getId(), query);
        
        if (query == null || query.trim().isEmpty()) {
            throw new BadRequestException("Search query cannot be empty");
        }
        
        try {
            List<AIConversation> conversations = conversationService.searchConversations(
                    currentUser.getId(), query);
            
            log.debug("Found {} conversations matching query", conversations.size());
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            log.error("Error searching conversations", e);
            throw new AIServiceException("Failed to search conversations: " + e.getMessage(), e);
        }
    }

    @GetMapping("/conversations/stats")
    public ResponseEntity<Map<String, Object>> getConversationStats(
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting conversation stats for user: {}", currentUser.getId());
        
        try {
            Map<String, Object> stats = conversationService.getConversationStats(currentUser.getId());
            
            log.debug("Retrieved conversation stats for user: {}", currentUser.getId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting conversation stats", e);
            throw new AIServiceException("Failed to get conversation stats: " + e.getMessage(), e);
        }
    }

    @PutMapping("/conversations/{conversationId}/title")
    public ResponseEntity<AIConversation> updateConversationTitle(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Updating title for conversation: {}", conversationId);
        
        String newTitle = request.get("title");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new BadRequestException("Title cannot be empty");
        }
        
        try {
            AIConversation conversation = conversationService.updateConversationTitle(
                    conversationId, currentUser.getId(), newTitle);
            
            log.info("Updated title for conversation: {}", conversationId);
            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            log.error("Error updating conversation title: {}", conversationId, e);
            throw new AIServiceException("Failed to update conversation title: " + e.getMessage(), e);
        }
    }

    // CODE EXECUTION ENDPOINT

    @PostMapping("/execute-code")
    public ResponseEntity<CodeExecutionService.CodeExecutionResult> executeCode(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Executing code for user: {}", currentUser.getId());
        
        String code = request.get("code");
        String input = request.get("input");
        
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Code cannot be empty");
        }
        
        try {
            CodeExecutionService.CodeExecutionResult result = codeExecutionService.executeJavaCode(code, input);
            
            // Award points for code execution
            if (result.isSuccess()) {
                pointAwardHelper.awardAIChatPoints(currentUser.getId(), "code-execution-success");
            }
            
            log.info("Code execution completed for user: {}, success: {}", 
                    currentUser.getId(), result.isSuccess());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error executing code for user: {}", currentUser.getId(), e);
            throw new AIServiceException("Failed to execute code: " + e.getMessage(), e);
        }
    }
} 