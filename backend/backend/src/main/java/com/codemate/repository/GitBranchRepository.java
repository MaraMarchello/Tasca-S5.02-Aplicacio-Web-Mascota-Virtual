package com.codemate.repository;

import com.codemate.model.GitBranch;
import com.codemate.model.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitBranchRepository extends JpaRepository<GitBranch, Long> {
    
    Optional<GitBranch> findByRepositoryAndName(GitRepository repository, String name);
    
    List<GitBranch> findByRepositoryOrderByName(GitRepository repository);
    
    List<GitBranch> findByRepositoryAndIsActive(GitRepository repository, Boolean isActive);
    
    @Query("SELECT gb FROM GitBranch gb WHERE gb.repository = :repository AND gb.isActive = true")
    Optional<GitBranch> findActiveByRepository(@Param("repository") GitRepository repository);
    
    @Query("SELECT gb FROM GitBranch gb WHERE gb.repository = :repository AND gb.isMerged = false ORDER BY gb.name")
    List<GitBranch> findUnmergedByRepository(@Param("repository") GitRepository repository);
    
    @Query("SELECT gb FROM GitBranch gb WHERE gb.repository = :repository AND gb.parentBranch = :parentBranch")
    List<GitBranch> findByRepositoryAndParentBranch(@Param("repository") GitRepository repository, @Param("parentBranch") String parentBranch);
    
    @Query("SELECT COUNT(gb) FROM GitBranch gb WHERE gb.repository = :repository")
    Long countByRepository(@Param("repository") GitRepository repository);
    
    @Query("SELECT COUNT(gb) FROM GitBranch gb WHERE gb.repository = :repository AND gb.isMerged = false")
    Long countUnmergedByRepository(@Param("repository") GitRepository repository);
    
    void deleteByRepository(GitRepository repository);
} 