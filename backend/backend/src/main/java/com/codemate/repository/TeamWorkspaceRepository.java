package com.codemate.repository;

import com.codemate.model.TeamWorkspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamWorkspaceRepository extends JpaRepository<TeamWorkspace, Long> {

    // Find workspaces owned by user
    @Query("SELECT w FROM TeamWorkspace w WHERE w.owner.id = :userId AND w.isActive = true")
    Page<TeamWorkspace> findByOwnerId(@Param("userId") Long userId, Pageable pageable);

    // Find workspaces where user is a member
    @Query("SELECT w FROM TeamWorkspace w JOIN w.members m WHERE m.user.id = :userId AND m.isActive = true AND w.isActive = true")
    Page<TeamWorkspace> findByMemberId(@Param("userId") Long userId, Pageable pageable);

    // Find all workspaces accessible to user (owned or member)
    @Query("SELECT DISTINCT w FROM TeamWorkspace w LEFT JOIN w.members m " +
           "WHERE (w.owner.id = :userId OR (m.user.id = :userId AND m.isActive = true)) " +
           "AND w.isActive = true")
    Page<TeamWorkspace> findAccessibleWorkspaces(@Param("userId") Long userId, Pageable pageable);

    // Find by invite code
    Optional<TeamWorkspace> findByInviteCodeAndIsActiveTrue(String inviteCode);

    // Find public workspaces
    @Query("SELECT w FROM TeamWorkspace w WHERE w.visibility = 'PUBLIC' AND w.isActive = true")
    Page<TeamWorkspace> findPublicWorkspaces(Pageable pageable);

    // Search workspaces by name or description
    @Query("SELECT DISTINCT w FROM TeamWorkspace w LEFT JOIN w.members m " +
           "WHERE (w.owner.id = :userId OR (m.user.id = :userId AND m.isActive = true)) " +
           "AND w.isActive = true " +
           "AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(w.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<TeamWorkspace> searchAccessibleWorkspaces(@Param("userId") Long userId, @Param("query") String query);

    // Count workspaces owned by user
    @Query("SELECT COUNT(w) FROM TeamWorkspace w WHERE w.owner.id = :userId AND w.isActive = true")
    long countByOwnerId(@Param("userId") Long userId);

    // Count workspaces where user is a member
    @Query("SELECT COUNT(DISTINCT w) FROM TeamWorkspace w JOIN w.members m " +
           "WHERE m.user.id = :userId AND m.isActive = true AND w.isActive = true")
    long countByMemberId(@Param("userId") Long userId);

    // Find workspaces with most members (for recommendations)
    @Query("SELECT w FROM TeamWorkspace w WHERE w.visibility = 'PUBLIC' AND w.isActive = true " +
           "ORDER BY SIZE(w.members) DESC")
    List<TeamWorkspace> findPopularPublicWorkspaces(Pageable pageable);
} 