package com.codemate.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointBalanceResponse {
    private Long currentPoints;
    private Long totalEarned;
    private Long totalSpent;
} 