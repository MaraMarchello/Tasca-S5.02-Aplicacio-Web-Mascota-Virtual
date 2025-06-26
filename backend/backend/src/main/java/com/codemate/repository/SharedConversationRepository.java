package com.codemate.repository;

import com.codemate.model.SharedConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SharedConversationRepository extends JpaRepository<SharedConversation, Long> {

    // Find shared conversations in a workspace
    @Query("SELECT sc FROM SharedConversation sc WHERE sc.workspace.id = :workspaceId " +
           "ORDER BY sc.isPinned DESC, sc.createdAt DESC")
    Page<SharedConversation> findByWorkspaceId(@Param("workspaceId") Long workspaceId, Pageable pageable);

    // Find shared conversations by user
    @Query("SELECT sc FROM SharedConversation sc WHERE sc.sharedBy.id = :userId " +
           "ORDER BY sc.createdAt DESC")
    Page<SharedConversation> findBySharedById(@Param("userId") Long userId, Pageable pageable);

    // Find pinned conversations in workspace
    @Query("SELECT sc FROM SharedConversation sc WHERE sc.workspace.id = :workspaceId AND sc.isPinned = true " +
           "ORDER BY sc.createdAt DESC")
    List<SharedConversation> findPinnedByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Search shared conversations in workspace
    @Query("SELECT sc FROM SharedConversation sc WHERE sc.workspace.id = :workspaceId " +
           "AND (LOWER(sc.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(sc.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY sc.isPinned DESC, sc.viewCount DESC")
    List<SharedConversation> searchInWorkspace(@Param("workspaceId") Long workspaceId, @Param("query") String query);

    // Find most viewed conversations in workspace
    @Query("SELECT sc FROM SharedConversation sc WHERE sc.workspace.id = :workspaceId " +
           "ORDER BY sc.viewCount DESC, sc.createdAt DESC")
    List<SharedConversation> findMostViewedInWorkspace(@Param("workspaceId") Long workspaceId, Pageable pageable);

    // Find by conversation ID and workspace ID
    Optional<SharedConversation> findByConversationIdAndWorkspaceId(Long conversationId, Long workspaceId);

    // Count shared conversations in workspace
    @Query("SELECT COUNT(sc) FROM SharedConversation sc WHERE sc.workspace.id = :workspaceId")
    long countByWorkspaceId(@Param("workspaceId") Long workspaceId);

    // Count shared conversations by user
    @Query("SELECT COUNT(sc) FROM SharedConversation sc WHERE sc.sharedBy.id = :userId")
    long countBySharedById(@Param("userId") Long userId);

    // Find recent conversations in workspace
    @Query("SELECT sc FROM SharedConversation sc WHERE sc.workspace.id = :workspaceId " +
           "ORDER BY sc.createdAt DESC")
    List<SharedConversation> findRecentInWorkspace(@Param("workspaceId") Long workspaceId, Pageable pageable);
} 