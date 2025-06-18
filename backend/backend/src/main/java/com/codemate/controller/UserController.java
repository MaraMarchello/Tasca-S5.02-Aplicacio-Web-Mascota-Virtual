package com.codemate.controller;


import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    /**
     * Get current user information
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        log.debug("Getting current user information for user ID: {}", userPrincipal.getId());
        
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", userPrincipal.getId());
        userInfo.put("name", userPrincipal.getName());
        userInfo.put("email", userPrincipal.getUsername()); // email is stored in username
        userInfo.put("authorities", userPrincipal.getAuthorities());
        
        return ResponseEntity.ok(userInfo);
    }


} 