package com.codemate.model;

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
@Table(name = "team_workspaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TeamWorkspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceVisibility visibility = WorkspaceVisibility.PRIVATE;

    @Column(name = "invite_code", unique = true, length = 20)
    private String inviteCode;

    @Column(name = "max_members")
    private Integer maxMembers = 10;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("workspace-members")
    private Set<TeamWorkspaceMember> members = new HashSet<>();

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("workspace-sharedConversations")
    private Set<SharedConversation> sharedConversations = new HashSet<>();

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum WorkspaceVisibility {
        PRIVATE,
        PUBLIC,
        INVITE_ONLY
    }

    // Helper methods
    public boolean isOwner(Long userId) {
        return owner != null && owner.getId().equals(userId);
    }

    public boolean isMember(Long userId) {
        return members.stream()
                .anyMatch(member -> member.getUser().getId().equals(userId) && member.isActive());
    }

    public boolean canAccess(Long userId) {
        return isOwner(userId) || isMember(userId);
    }

    public int getMemberCount() {
        return (int) members.stream().filter(TeamWorkspaceMember::isActive).count();
    }

    public boolean canAddMembers() {
        return maxMembers == null || getMemberCount() < maxMembers;
    }
} 