package com.codemate.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PurchaseItemRequest {
    
    @NotNull(message = "Item template ID is required")
    @Positive(message = "Item template ID must be positive")
    private Long itemTemplateId;
} 