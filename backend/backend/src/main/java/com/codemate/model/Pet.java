package com.codemate.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.*;

@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User owner;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetType type;
    
    // Simplified stats - just happiness (0-100)
    @Column(nullable = false)
    private Integer happiness = 100;
    
    // Total points earned by the user (for display)
    @Column(nullable = false)
    private Long totalPointsEarned = 0L;
    
    @Column(nullable = false)
    private Date lastFed;
    
    @Column(nullable = false)
    private Date createdAt;
    
    @Column
    private Date updatedAt;
    
    // Simplified - only owned items (no complex inventory)
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<PetItem> items = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        if (lastFed == null) {
            lastFed = new Date();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
} 