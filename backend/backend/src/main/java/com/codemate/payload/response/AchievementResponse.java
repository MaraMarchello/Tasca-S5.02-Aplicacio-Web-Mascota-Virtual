package com.codemate.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AchievementResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer targetValue;
    private Long pointsReward;
    private String badgeImageUrl;
    private Boolean active;
} 