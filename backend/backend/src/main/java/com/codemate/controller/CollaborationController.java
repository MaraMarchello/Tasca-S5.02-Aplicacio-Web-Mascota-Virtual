package com.codemate.controller;

import com.codemate.model.SharedConversation;
import com.codemate.model.TeamWorkspace;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.CollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/collaboration")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collaborationService;

    @PostMapping("/workspaces")
    public ResponseEntity<TeamWorkspace> createWorkspace(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Creating workspace for user: {}", currentUser.getId());
        
        String name = request.get("name");
        String description = request.get("description");
        String visibilityStr = request.getOrDefault("visibility", "PRIVATE");
        
        TeamWorkspace.WorkspaceVisibility visibility = 
                TeamWorkspace.WorkspaceVisibility.valueOf(visibilityStr.toUpperCase());
        
        TeamWorkspace workspace = collaborationService.createWorkspace(
                name, description, visibility, currentUser.getId());
        
        return ResponseEntity.ok(workspace);
    }

    @PostMapping("/workspaces/join")
    public ResponseEntity<TeamWorkspace> joinWorkspace(
            @RequestBody Map<String, String> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("User {} joining workspace", currentUser.getId());
        
        String inviteCode = request.get("inviteCode");
        
        TeamWorkspace workspace = collaborationService.joinWorkspaceByInviteCode(
                inviteCode, currentUser.getId());
        
        return ResponseEntity.ok(workspace);
    }

    @GetMapping("/workspaces")
    public ResponseEntity<Page<TeamWorkspace>> getUserWorkspaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting workspaces for user: {}", currentUser.getId());
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TeamWorkspace> workspaces = collaborationService.getUserWorkspaces(
                currentUser.getId(), pageable);
        
        return ResponseEntity.ok(workspaces);
    }

    @PostMapping("/conversations/share")
    public ResponseEntity<SharedConversation> shareConversation(
            @RequestBody Map<String, Object> request,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Sharing conversation for user: {}", currentUser.getId());
        
        Long conversationId = ((Number) request.get("conversationId")).longValue();
        Long workspaceId = ((Number) request.get("workspaceId")).longValue();
        String title = (String) request.get("title");
        String description = (String) request.get("description");
        String permissionStr = (String) request.getOrDefault("permission", "VIEW_ONLY");
        
        SharedConversation.SharePermission permission = 
                SharedConversation.SharePermission.valueOf(permissionStr.toUpperCase());
        
        SharedConversation sharedConversation = collaborationService.shareConversation(
                conversationId, workspaceId, title, description, permission, currentUser.getId());
        
        return ResponseEntity.ok(sharedConversation);
    }

    @GetMapping("/workspaces/{workspaceId}/conversations")
    public ResponseEntity<Page<SharedConversation>> getWorkspaceConversations(
            @PathVariable Long workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting conversations for workspace: {}", workspaceId);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SharedConversation> conversations = collaborationService.getWorkspaceConversations(
                workspaceId, currentUser.getId(), pageable);
        
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{sharedConversationId}")
    public ResponseEntity<SharedConversation> viewSharedConversation(
            @PathVariable Long sharedConversationId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Viewing shared conversation: {}", sharedConversationId);
        
        SharedConversation sharedConversation = collaborationService.viewSharedConversation(
                sharedConversationId, currentUser.getId());
        
        return ResponseEntity.ok(sharedConversation);
    }

    @GetMapping("/workspaces/search")
    public ResponseEntity<List<TeamWorkspace>> searchWorkspaces(
            @RequestParam String query,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Searching workspaces for user: {} with query: {}", currentUser.getId(), query);
        
        List<TeamWorkspace> workspaces = collaborationService.searchWorkspaces(
                query, currentUser.getId());
        
        return ResponseEntity.ok(workspaces);
    }

    @GetMapping("/workspaces/{workspaceId}/stats")
    public ResponseEntity<CollaborationService.WorkspaceStats> getWorkspaceStats(
            @PathVariable Long workspaceId,
            @CurrentUser UserPrincipal currentUser) {
        log.info("Getting stats for workspace: {}", workspaceId);
        
        CollaborationService.WorkspaceStats stats = collaborationService.getWorkspaceStats(
                workspaceId, currentUser.getId());
        
        return ResponseEntity.ok(stats);
    }
} 