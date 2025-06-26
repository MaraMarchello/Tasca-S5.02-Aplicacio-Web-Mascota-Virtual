package com.codemate.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "git_commands")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class GitCommand {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String command;
    
    @Column(columnDefinition = "TEXT")
    private String output;
    
    @Column(columnDefinition = "TEXT")
    private String errorOutput;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean successful = true;
    
    @Column(nullable = false)
    private Integer exitCode;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column
    private String scenarioId;
    
    @Column
    private Integer stepNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    @JsonBackReference("repository-commands")
    private GitRepository repository;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime executedAt;
} 