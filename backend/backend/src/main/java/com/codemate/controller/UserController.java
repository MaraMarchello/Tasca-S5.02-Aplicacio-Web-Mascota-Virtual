package com.codemate.controller;

import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.User;
import com.codemate.repository.UserRepository;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public User getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        log.info("Fetching current user profile for user ID: {}", userPrincipal.getId());
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userPrincipal.getId());
                    return new ResourceNotFoundException("User", "id", userPrincipal.getId());
                });
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public User getUserById(@PathVariable Long userId) {
        log.info("Admin fetching user profile for user ID: {}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found with ID: {}", userId);
                    return new ResourceNotFoundException("User", "id", userId);
                });
    }
} 