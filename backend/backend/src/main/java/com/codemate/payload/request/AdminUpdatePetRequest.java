package com.codemate.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUpdatePetRequest {
    
    @Size(min = 1, max = 50, message = "Pet name must be between 1 and 50 characters")
    private String name;
    
    @Min(value = 0, message = "Happiness must be between 0 and 100")
    @Max(value = 100, message = "Happiness must be between 0 and 100")
    private Integer happiness;
} 