package com.codemate.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@Entity
@Table(name = "item_templates")
@Getter
@Setter
@NoArgsConstructor
public class ItemTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;
    
    @Column(nullable = false)
    private Long price;
    
    @Column
    private String imageUrl;
    
    @Column(nullable = false)
    private Boolean available = true;
    
    // Simple effect - only happiness boost
    @Column
    private Integer happinessBoost = 0;
    
    @Column(nullable = false)
    private Date createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
} 