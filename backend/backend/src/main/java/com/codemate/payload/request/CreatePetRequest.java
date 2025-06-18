package com.codemate.payload.request;

import com.codemate.model.PetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePetRequest {
    
    @NotBlank(message = "Pet name cannot be empty")
    @Size(min = 1, max = 50, message = "Pet name must be between 1 and 50 characters")
    private String name;
    
    @NotNull(message = "Pet type is required")
    private PetType type;
} 