package com.codemate.repository;

import com.codemate.model.ItemTemplate;
import com.codemate.model.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemTemplateRepository extends JpaRepository<ItemTemplate, Long> {
    
    // Find available items for shop
    List<ItemTemplate> findByAvailableTrue();
    
    // Find items by type
    List<ItemTemplate> findByTypeAndAvailableTrue(ItemType type);
    
    // Find item by name
    Optional<ItemTemplate> findByName(String name);
    
    // Find items by price range
    List<ItemTemplate> findByPriceBetweenAndAvailableTrue(Long minPrice, Long maxPrice);
    
    // Find items that boost happiness
    List<ItemTemplate> findByHappinessBoostGreaterThanAndAvailableTrue(Integer minBoost);
} 