package com.codemate.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePetNameRequest {
    
    @NotBlank(message = "Pet name cannot be empty")
    @Size(min = 1, max = 50, message = "Pet name must be between 1 and 50 characters")
    private String name;
} 