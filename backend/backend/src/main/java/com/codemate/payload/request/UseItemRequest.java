package com.codemate.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class UseItemRequest {
    
    @NotNull(message = "Pet item ID is required")
    @Positive(message = "Pet item ID must be positive")
    private Long petItemId;
} 