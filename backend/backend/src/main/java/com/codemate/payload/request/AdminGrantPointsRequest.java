package com.codemate.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AdminGrantPointsRequest {
    
    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be positive")
    private Long userId;
    
    @NotNull(message = "Point amount is required")
    @Positive(message = "Point amount must be positive")
    private Long amount;
    
    @NotBlank(message = "Reason is required")
    private String reason;
} 