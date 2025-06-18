package com.codemate.controller;

import com.codemate.model.ItemTemplate;
import com.codemate.model.ItemType;
import com.codemate.model.PetItem;
import com.codemate.payload.DataResponse;
import com.codemate.payload.request.PurchaseItemRequest;
import com.codemate.payload.request.UseItemRequest;
import com.codemate.payload.response.ItemTemplateResponse;
import com.codemate.payload.response.PetItemResponse;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.ShopService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/shop")
@PreAuthorize("hasRole('USER')")
public class ShopController {
    
    private final ShopService shopService;
    
    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }
    
    @GetMapping("/items")
    public ResponseEntity<DataResponse<List<ItemTemplateResponse>>> getAvailableItems() {
        
        log.debug("Getting available shop items");
        
        List<ItemTemplate> items = shopService.getAvailableItems();
        List<ItemTemplateResponse> response = items.stream()
                .map(this::convertToItemTemplateResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/items/food")
    public ResponseEntity<DataResponse<List<ItemTemplateResponse>>> getFoodItems() {
        
        log.debug("Getting available food items");
        
        List<ItemTemplate> items = shopService.getAvailableItemsByType(ItemType.FOOD);
        List<ItemTemplateResponse> response = items.stream()
                .map(this::convertToItemTemplateResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/items/accessories")
    public ResponseEntity<DataResponse<List<ItemTemplateResponse>>> getAccessoryItems() {
        
        log.debug("Getting available accessory items");
        
        List<ItemTemplate> items = shopService.getAvailableItemsByType(ItemType.ACCESSORY);
        List<ItemTemplateResponse> response = items.stream()
                .map(this::convertToItemTemplateResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @PostMapping("/purchase")
    public ResponseEntity<DataResponse<PetItemResponse>> purchaseItem(
            @Valid @RequestBody PurchaseItemRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("User {} purchasing item with template ID: {}", 
                userPrincipal.getId(), request.getItemTemplateId());
        
        PetItem petItem = shopService.purchaseItem(userPrincipal.getId(), request.getItemTemplateId());
        PetItemResponse response = convertToPetItemResponse(petItem);
        
        return ResponseEntity.ok(DataResponse.success("Item purchased successfully", response));
    }
    
    @GetMapping("/my-items")
    public ResponseEntity<DataResponse<List<PetItemResponse>>> getMyItems(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting inventory for user: {}", userPrincipal.getId());
        
        List<PetItem> items = shopService.getPetInventory(userPrincipal.getId());
        List<PetItemResponse> response = items.stream()
                .map(this::convertToPetItemResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/my-items/equipped")
    public ResponseEntity<DataResponse<List<PetItemResponse>>> getEquippedItems(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting equipped items for user: {}", userPrincipal.getId());
        
        List<PetItem> items = shopService.getEquippedItems(userPrincipal.getId());
        List<PetItemResponse> response = items.stream()
                .map(this::convertToPetItemResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @PostMapping("/use-item")
    public ResponseEntity<DataResponse<String>> useItem(
            @Valid @RequestBody UseItemRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("User {} using item with ID: {}", userPrincipal.getId(), request.getPetItemId());
        
        shopService.useItem(userPrincipal.getId(), request.getPetItemId());
        
        return ResponseEntity.ok(DataResponse.success("Item used successfully", "Item consumed"));
    }
    
    @PostMapping("/equip/{petItemId}")
    public ResponseEntity<DataResponse<PetItemResponse>> toggleEquipItem(
            @PathVariable Long petItemId,
            @CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("User {} toggling equipment for item ID: {}", userPrincipal.getId(), petItemId);
        
        PetItem petItem = shopService.toggleEquipItem(userPrincipal.getId(), petItemId);
        PetItemResponse response = convertToPetItemResponse(petItem);
        
        String message = petItem.getEquipped() ? "Item equipped successfully" : "Item unequipped successfully";
        return ResponseEntity.ok(DataResponse.success(message, response));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<DataResponse<ShopService.ShopStats>> getShopStats(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting shop statistics for user: {}", userPrincipal.getId());
        
        ShopService.ShopStats stats = shopService.getShopStats(userPrincipal.getId());
        
        return ResponseEntity.ok(DataResponse.success(stats));
    }
    
    // Helper methods
    private ItemTemplateResponse convertToItemTemplateResponse(ItemTemplate item) {
        return new ItemTemplateResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getType(),
                item.getPrice(),
                item.getImageUrl(),
                item.getHappinessBoost(),
                item.getAvailable()
        );
    }
    
    private PetItemResponse convertToPetItemResponse(PetItem petItem) {
        ItemTemplateResponse itemTemplate = convertToItemTemplateResponse(petItem.getItemTemplate());
        return new PetItemResponse(
                petItem.getId(),
                itemTemplate,
                petItem.getQuantity(),
                petItem.getEquipped(),
                petItem.getAcquiredAt()
        );
    }
} 