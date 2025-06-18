package com.codemate.payload;

import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String name;
    private String email;
    private Set<String> roles;

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public AuthResponse(String accessToken, Long userId, String name, String email, Set<String> roles) {
        this.accessToken = accessToken;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.roles = roles;
    }
} 