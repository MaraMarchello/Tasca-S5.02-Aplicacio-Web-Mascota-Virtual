package com.codemate.service;

import com.codemate.exception.BadRequestException;
import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.*;
import com.codemate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CollaborationService {

    private final TeamWorkspaceRepository workspaceRepository;
    private final SharedConversationRepository sharedConversationRepository;
    private final AIConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final PointAwardHelper pointAwardHelper;
    
    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 8;

    /**
     * Create a new team workspace
     */
    public TeamWorkspace createWorkspace(String name, String description, 
                                       TeamWorkspace.WorkspaceVisibility visibility, Long ownerId) {
        log.info("Creating workspace: {} for user: {}", name, ownerId);
        
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", ownerId));
        
        TeamWorkspace workspace = new TeamWorkspace();
        workspace.setName(name);
        workspace.setDescription(description);
        workspace.setOwner(owner);
        workspace.setVisibility(visibility);
        workspace.setInviteCode(generateInviteCode());
        
        workspace = workspaceRepository.save(workspace);
        
        // Create owner membership
        TeamWorkspaceMember ownerMember = new TeamWorkspaceMember();
        ownerMember.setWorkspace(workspace);
        ownerMember.setUser(owner);
        ownerMember.setRole(TeamWorkspaceMember.MemberRole.OWNER);
        ownerMember.setJoinedAt(LocalDateTime.now());
        
        // Award points for creating workspace
        pointAwardHelper.awardAIChatPoints(ownerId, "workspace-creation");
        
        log.info("Created workspace: {} with ID: {}", name, workspace.getId());
        return workspace;
    }

    /**
     * Join workspace by invite code
     */
    public TeamWorkspace joinWorkspaceByInviteCode(String inviteCode, Long userId) {
        log.info("User {} joining workspace with invite code: {}", userId, inviteCode);
        
        TeamWorkspace workspace = workspaceRepository.findByInviteCodeAndIsActiveTrue(inviteCode)
                .orElseThrow(() -> new BadRequestException("Invalid invite code"));
        
        if (!workspace.canAddMembers()) {
            throw new BadRequestException("Workspace is at maximum capacity");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Check if already a member
        if (workspace.isMember(userId)) {
            throw new BadRequestException("User is already a member of this workspace");
        }
        
        TeamWorkspaceMember member = new TeamWorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(TeamWorkspaceMember.MemberRole.MEMBER);
        member.setJoinedAt(LocalDateTime.now());
        
        // Award points for joining workspace
        pointAwardHelper.awardAIChatPoints(userId, "workspace-join");
        
        log.info("User {} joined workspace: {}", userId, workspace.getName());
        return workspace;
    }

    /**
     * Share conversation with workspace
     */
    public SharedConversation shareConversation(Long conversationId, Long workspaceId, 
                                              String title, String description,
                                              SharedConversation.SharePermission permission, Long userId) {
        log.info("Sharing conversation {} to workspace {} by user {}", conversationId, workspaceId, userId);
        
        // Verify conversation ownership
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        AIConversation conversation = conversationRepository.findByIdAndUser(conversationId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        
        // Verify workspace access
        TeamWorkspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        
        if (!workspace.canAccess(userId)) {
            throw new BadRequestException("No access to workspace");
        }
        
        // Check if already shared
        Optional<SharedConversation> existing = sharedConversationRepository
                .findByConversationIdAndWorkspaceId(conversationId, workspaceId);
        if (existing.isPresent()) {
            throw new BadRequestException("Conversation already shared to this workspace");
        }
        
        SharedConversation sharedConversation = new SharedConversation();
        sharedConversation.setConversation(conversation);
        sharedConversation.setWorkspace(workspace);
        sharedConversation.setSharedBy(user);
        sharedConversation.setTitle(title);
        sharedConversation.setDescription(description);
        sharedConversation.setPermission(permission);
        
        sharedConversation = sharedConversationRepository.save(sharedConversation);
        
        // Award points for sharing
        pointAwardHelper.awardAIChatPoints(userId, "conversation-share");
        
        log.info("Shared conversation {} to workspace {}", conversationId, workspaceId);
        return sharedConversation;
    }

    /**
     * Get user's accessible workspaces
     */
    public Page<TeamWorkspace> getUserWorkspaces(Long userId, Pageable pageable) {
        return workspaceRepository.findAccessibleWorkspaces(userId, pageable);
    }

    /**
     * Get shared conversations in workspace
     */
    public Page<SharedConversation> getWorkspaceConversations(Long workspaceId, Long userId, Pageable pageable) {
        // Verify workspace access
        TeamWorkspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        
        if (!workspace.canAccess(userId)) {
            throw new BadRequestException("No access to workspace");
        }
        
        return sharedConversationRepository.findByWorkspaceId(workspaceId, pageable);
    }

    /**
     * View shared conversation
     */
    public SharedConversation viewSharedConversation(Long sharedConversationId, Long userId) {
        SharedConversation sharedConversation = sharedConversationRepository.findById(sharedConversationId)
                .orElseThrow(() -> new ResourceNotFoundException("SharedConversation", "id", sharedConversationId));
        
        if (!sharedConversation.canView(userId)) {
            throw new BadRequestException("No access to this conversation");
        }
        
        // Increment view count
        sharedConversation.incrementViewCount();
        sharedConversationRepository.save(sharedConversation);
        
        return sharedConversation;
    }

    /**
     * Search workspaces
     */
    public List<TeamWorkspace> searchWorkspaces(String query, Long userId) {
        return workspaceRepository.searchAccessibleWorkspaces(userId, query);
    }

    /**
     * Get workspace statistics
     */
    public WorkspaceStats getWorkspaceStats(Long workspaceId, Long userId) {
        TeamWorkspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", "id", workspaceId));
        
        if (!workspace.canAccess(userId)) {
            throw new BadRequestException("No access to workspace");
        }
        
        WorkspaceStats stats = new WorkspaceStats();
        stats.setWorkspaceId(workspaceId);
        stats.setMemberCount(workspace.getMemberCount());
        stats.setSharedConversationCount(sharedConversationRepository.countByWorkspaceId(workspaceId));
        stats.setRecentConversations(sharedConversationRepository.findRecentInWorkspace(workspaceId, 
                Pageable.ofSize(5)).size());
        
        return stats;
    }

    /**
     * Generate unique invite code
     */
    private String generateInviteCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
        
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            code.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }
        
        // Ensure uniqueness
        String inviteCode = code.toString();
        while (workspaceRepository.findByInviteCodeAndIsActiveTrue(inviteCode).isPresent()) {
            code = new StringBuilder(INVITE_CODE_LENGTH);
            for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
                code.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
            }
            inviteCode = code.toString();
        }
        
        return inviteCode;
    }

    /**
     * Workspace statistics data class
     */
    public static class WorkspaceStats {
        private Long workspaceId;
        private int memberCount;
        private long sharedConversationCount;
        private int recentConversations;
        
        // Getters and setters
        public Long getWorkspaceId() { return workspaceId; }
        public void setWorkspaceId(Long workspaceId) { this.workspaceId = workspaceId; }
        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
        public long getSharedConversationCount() { return sharedConversationCount; }
        public void setSharedConversationCount(long sharedConversationCount) { this.sharedConversationCount = sharedConversationCount; }
        public int getRecentConversations() { return recentConversations; }
        public void setRecentConversations(int recentConversations) { this.recentConversations = recentConversations; }
    }
} 