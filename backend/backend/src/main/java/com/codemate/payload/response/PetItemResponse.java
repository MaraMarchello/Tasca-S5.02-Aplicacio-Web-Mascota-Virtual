package com.codemate.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PetItemResponse {
    private Long id;
    private ItemTemplateResponse itemTemplate;
    private Integer quantity;
    private Boolean equipped;
    private Date acquiredAt;
} 