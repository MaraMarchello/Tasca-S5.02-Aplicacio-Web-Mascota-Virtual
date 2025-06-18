package com.codemate.service;

import com.codemate.exception.BadRequestException;
import com.codemate.exception.InsufficientPointsException;
import com.codemate.exception.ItemNotFoundException;
import com.codemate.exception.PetNotFoundException;
import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.*;
import com.codemate.repository.ItemTemplateRepository;
import com.codemate.repository.PetItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ShopService {
    
    private final ItemTemplateRepository itemTemplateRepository;
    private final PetItemRepository petItemRepository;
    private final PetService petService;
    private final PointTransactionService pointTransactionService;
    private final AchievementService achievementService;
    
    public ShopService(ItemTemplateRepository itemTemplateRepository,
                      PetItemRepository petItemRepository,
                      PetService petService,
                      PointTransactionService pointTransactionService,
                      AchievementService achievementService) {
        this.itemTemplateRepository = itemTemplateRepository;
        this.petItemRepository = petItemRepository;
        this.petService = petService;
        this.pointTransactionService = pointTransactionService;
        this.achievementService = achievementService;
    }
    
    /**
     * Get all available items in the shop
     */
    @Transactional(readOnly = true)
    public List<ItemTemplate> getAvailableItems() {
        return itemTemplateRepository.findByAvailableTrue();
    }
    
    /**
     * Get available items by type
     */
    @Transactional(readOnly = true)
    public List<ItemTemplate> getAvailableItemsByType(ItemType type) {
        return itemTemplateRepository.findByTypeAndAvailableTrue(type);
    }
    
    /**
     * Purchase an item for the user's pet
     */
    public PetItem purchaseItem(Long userId, Long itemTemplateId) {
        // Get user's pet
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> PetNotFoundException.forUser(userId));
        
        // Get item template
        ItemTemplate itemTemplate = itemTemplateRepository.findById(itemTemplateId)
            .orElseThrow(() -> ItemNotFoundException.forId(itemTemplateId));
        
        if (!itemTemplate.getAvailable()) {
            throw new BadRequestException("Item is not available for purchase");
        }
        
        // Check if user has enough points
        Long currentPoints = pointTransactionService.getCurrentPoints(userId);
        if (currentPoints < itemTemplate.getPrice()) {
            throw InsufficientPointsException.forPurchase(itemTemplate.getPrice(), currentPoints);
        }
        
        // Create point transaction for purchase
        pointTransactionService.createTransaction(userId, TransactionType.SPENT,
            PointSource.ITEM_PURCHASE, itemTemplate.getPrice(),
            "Purchased " + itemTemplate.getName(), itemTemplate.getId().toString());
        
        // Check if user already has this item
        Optional<PetItem> existingItem = petItemRepository.findByPetIdAndItemTemplateId(
            pet.getId(), itemTemplateId);
        
        PetItem petItem;
        if (existingItem.isPresent()) {
            // Increase quantity
            petItem = existingItem.get();
            petItem.setQuantity(petItem.getQuantity() + 1);
        } else {
            // Create new pet item
            petItem = new PetItem();
            petItem.setPet(pet);
            petItem.setItemTemplate(itemTemplate);
            petItem.setQuantity(1);
            petItem.setEquipped(false);
            petItem.setAcquiredAt(new Date());
        }
        
        PetItem savedItem = petItemRepository.save(petItem);
        
        // Track shopping achievement
        achievementService.trackShopping(userId);
        
        return savedItem;
    }
    
    /**
     * Use/consume an item (for food items)
     */
    public void useItem(Long userId, Long petItemId) {
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "userId", userId));
        
        PetItem petItem = petItemRepository.findById(petItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PetItem", "id", petItemId));
        
        // Validate ownership
        if (!petItem.getPet().getId().equals(pet.getId())) {
            throw new BadRequestException("You can only use items from your own pet's inventory");
        }
        
        if (petItem.getQuantity() <= 0) {
            throw new BadRequestException("No items available to use");
        }
        
        ItemTemplate itemTemplate = petItem.getItemTemplate();
        
        // Apply item effects
        if (itemTemplate.getType() == ItemType.FOOD) {
            // Food items increase happiness and are consumed
            if (itemTemplate.getHappinessBoost() > 0) {
                petService.updateHappiness(pet.getId(), itemTemplate.getHappinessBoost());
            }
            
            // Decrease quantity
            petItem.setQuantity(petItem.getQuantity() - 1);
            
            if (petItem.getQuantity() <= 0) {
                petItemRepository.delete(petItem);
            } else {
                petItemRepository.save(petItem);
            }
        } else {
            throw new BadRequestException("Only food items can be consumed");
        }
    }
    
    /**
     * Equip/unequip an accessory item
     */
    public PetItem toggleEquipItem(Long userId, Long petItemId) {
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "userId", userId));
        
        PetItem petItem = petItemRepository.findById(petItemId)
            .orElseThrow(() -> new ResourceNotFoundException("PetItem", "id", petItemId));
        
        // Validate ownership
        if (!petItem.getPet().getId().equals(pet.getId())) {
            throw new BadRequestException("You can only equip items from your own pet's inventory");
        }
        
        if (petItem.getItemTemplate().getType() != ItemType.ACCESSORY) {
            throw new BadRequestException("Only accessory items can be equipped");
        }
        
        if (petItem.getEquipped()) {
            // Unequip
            petItem.setEquipped(false);
        } else {
            // Unequip other accessories of the same type first (only one accessory at a time)
            petItemRepository.unequipItemsByPetIdAndType(pet.getId(), ItemType.ACCESSORY);
            
            // Equip this item
            petItem.setEquipped(true);
        }
        
        return petItemRepository.save(petItem);
    }
    
    /**
     * Get pet's inventory
     */
    @Transactional(readOnly = true)
    public List<PetItem> getPetInventory(Long userId) {
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "userId", userId));
        
        return petItemRepository.findByPetId(pet.getId());
    }
    
    /**
     * Get pet's equipped items
     */
    @Transactional(readOnly = true)
    public List<PetItem> getEquippedItems(Long userId) {
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "userId", userId));
        
        return petItemRepository.findByPetIdAndEquippedTrue(pet.getId());
    }
    
    /**
     * Get items by type for a pet
     */
    @Transactional(readOnly = true)
    public List<PetItem> getPetItemsByType(Long userId, ItemType type) {
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "userId", userId));
        
        return petItemRepository.findByPetIdAndItemType(pet.getId(), type);
    }
    
    /**
     * Get shopping statistics for a user
     */
    @Transactional(readOnly = true)
    public ShopStats getShopStats(Long userId) {
        Pet pet = petService.getPetByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "userId", userId));
        
        long distinctItems = petItemRepository.countDistinctItemsByPetId(pet.getId());
        Long totalItems = petItemRepository.sumQuantityByPetId(pet.getId());
        
        return new ShopStats(distinctItems, totalItems != null ? totalItems : 0L);
    }
    
    // Inner class for statistics
    public static class ShopStats {
        private final long distinctItemsOwned;
        private final long totalItemsOwned;
        
        public ShopStats(long distinctItemsOwned, long totalItemsOwned) {
            this.distinctItemsOwned = distinctItemsOwned;
            this.totalItemsOwned = totalItemsOwned;
        }
        
        public long getDistinctItemsOwned() { return distinctItemsOwned; }
        public long getTotalItemsOwned() { return totalItemsOwned; }
    }
} 