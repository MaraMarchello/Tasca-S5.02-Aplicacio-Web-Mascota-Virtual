package com.codemate.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "shared_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SharedConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "messages"})
    private AIConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonBackReference("workspace-sharedConversations")
    private TeamWorkspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User sharedBy;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePermission permission = SharePermission.VIEW_ONLY;

    @Column(name = "is_pinned")
    private boolean isPinned = false;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @OneToMany(mappedBy = "sharedConversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("sharedConversation-comments")
    private Set<ConversationComment> comments = new HashSet<>();

    @OneToMany(mappedBy = "sharedConversation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("sharedConversation-reactions")
    private Set<ConversationReaction> reactions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SharePermission {
        VIEW_ONLY,
        COMMENT_ONLY,
        FULL_ACCESS
    }

    // Helper methods
    public boolean canView(Long userId) {
        return workspace.canAccess(userId);
    }

    public boolean canComment(Long userId) {
        return canView(userId) && 
               (permission == SharePermission.COMMENT_ONLY || permission == SharePermission.FULL_ACCESS);
    }

    public boolean canEdit(Long userId) {
        return canView(userId) && 
               (permission == SharePermission.FULL_ACCESS || sharedBy.getId().equals(userId));
    }

    public void incrementViewCount() {
        this.viewCount = (this.viewCount == null ? 0L : this.viewCount) + 1;
    }
} 