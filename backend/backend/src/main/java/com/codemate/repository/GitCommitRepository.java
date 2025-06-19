package com.codemate.repository;

import com.codemate.model.GitCommit;
import com.codemate.model.GitRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, Long> {
    
    Optional<GitCommit> findByHash(String hash);
    
    List<GitCommit> findByRepositoryOrderByCommitTimeDesc(GitRepository repository);
    
    List<GitCommit> findByRepositoryAndBranchNameOrderByCommitTimeDesc(GitRepository repository, String branchName);
    
    List<GitCommit> findByRepositoryIdOrderByCommitTimeDesc(Long repositoryId);
    
    @Query("SELECT gc FROM GitCommit gc WHERE gc.repository = :repository AND gc.branchName = :branchName ORDER BY gc.commitTime ASC")
    List<GitCommit> findByRepositoryAndBranchNameOrderByCommitTimeAsc(@Param("repository") GitRepository repository, @Param("branchName") String branchName);
    
    @Query("SELECT gc FROM GitCommit gc WHERE gc.repository.id = :repositoryId AND gc.hash IN :hashes")
    List<GitCommit> findByRepositoryIdAndHashIn(@Param("repositoryId") Long repositoryId, @Param("hashes") List<String> hashes);
    
    @Query("SELECT COUNT(gc) FROM GitCommit gc WHERE gc.repository = :repository")
    Long countByRepository(@Param("repository") GitRepository repository);
    
    @Query("SELECT COUNT(gc) FROM GitCommit gc WHERE gc.repository = :repository AND gc.branchName = :branchName")
    Long countByRepositoryAndBranchName(@Param("repository") GitRepository repository, @Param("branchName") String branchName);
    
    void deleteByRepository(GitRepository repository);
} 