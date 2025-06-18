package com.codemate.repository;

import com.codemate.model.PetItem;
import com.codemate.model.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetItemRepository extends JpaRepository<PetItem, Long> {
    
    // Find all items for a pet
    List<PetItem> findByPetId(Long petId);
    
    // Find specific item for a pet
    Optional<PetItem> findByPetIdAndItemTemplateId(Long petId, Long itemTemplateId);
    
    // Find equipped items for a pet
    List<PetItem> findByPetIdAndEquippedTrue(Long petId);
    
    // Find items by type for a pet
    @Query("SELECT pi FROM PetItem pi JOIN pi.itemTemplate it WHERE pi.pet.id = :petId AND it.type = :type")
    List<PetItem> findByPetIdAndItemType(@Param("petId") Long petId, @Param("type") ItemType type);
    
    // Count distinct items for a pet (for achievements)
    @Query("SELECT COUNT(DISTINCT pi.itemTemplate.id) FROM PetItem pi WHERE pi.pet.id = :petId")
    long countDistinctItemsByPetId(@Param("petId") Long petId);
    
    // Count total items for a pet
    @Query("SELECT SUM(pi.quantity) FROM PetItem pi WHERE pi.pet.id = :petId")
    Long sumQuantityByPetId(@Param("petId") Long petId);
    
    // Unequip items of specific type for a pet (for accessories)
    @Modifying
    @Query("UPDATE PetItem pi SET pi.equipped = false WHERE pi.pet.id = :petId AND pi.itemTemplate.type = :type")
    void unequipItemsByPetIdAndType(@Param("petId") Long petId, @Param("type") ItemType type);
} 