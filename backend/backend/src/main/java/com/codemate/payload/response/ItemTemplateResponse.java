package com.codemate.payload.response;

import com.codemate.model.ItemType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private ItemType type;
    private Long price;
    private String imageUrl;
    private Integer happinessBoost;
    private Boolean available;
} 