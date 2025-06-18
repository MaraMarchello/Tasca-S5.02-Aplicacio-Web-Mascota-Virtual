package com.codemate.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Date;

@Entity
@Table(name = "pet_items")
@Getter
@Setter
@NoArgsConstructor
public class PetItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_template_id", nullable = false)
    private ItemTemplate itemTemplate;
    
    @Column(nullable = false)
    private Integer quantity = 1;
    
    @Column(nullable = false)
    private Boolean equipped = false;
    
    @Column(nullable = false)
    private Date acquiredAt;
    
    @PrePersist
    protected void onCreate() {
        if (acquiredAt == null) {
            acquiredAt = new Date();
        }
    }
} 