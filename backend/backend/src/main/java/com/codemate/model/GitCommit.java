package com.codemate.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "git_commits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GitCommit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String hash;
    
    @Column(nullable = false)
    private String message;
    
    @Column(nullable = false)
    private String author;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String branchName;
    
    @ElementCollection
    @CollectionTable(name = "git_commit_parents", joinColumns = @JoinColumn(name = "commit_id"))
    @Column(name = "parent_hash")
    private List<String> parentHashes;
    
    @ElementCollection
    @CollectionTable(name = "git_commit_files", joinColumns = @JoinColumn(name = "commit_id"))
    @Column(name = "file_path")
    private List<String> modifiedFiles;
    
    @Column(columnDefinition = "TEXT")
    private String changes; // JSON representation of file changes
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitRepository repository;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime commitTime;
} 