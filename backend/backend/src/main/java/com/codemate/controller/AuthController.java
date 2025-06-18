package com.codemate.controller;

import com.codemate.exception.BadRequestException;
import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.Role;
import com.codemate.model.RoleType;
import com.codemate.model.User;
import com.codemate.payload.ApiResponse;
import com.codemate.payload.AuthResponse;
import com.codemate.payload.LoginRequest;
import com.codemate.payload.SignUpRequest;
import com.codemate.repository.RoleRepository;
import com.codemate.repository.UserRepository;
import com.codemate.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Login endpoint for API clients
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Processing login request for user: {}", loginRequest.getEmail());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Generate JWT token
            String jwt = tokenProvider.generateToken(authentication);
            log.debug("JWT token generated successfully for user: {}", loginRequest.getEmail());

            // Return JWT token
            return ResponseEntity.ok(new AuthResponse(jwt));
            
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", loginRequest.getEmail());
            throw e;
        } catch (AuthenticationException e) {
            log.error("Authentication error for user: {}", loginRequest.getEmail(), e);
            throw e;
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        log.info("Processing signup request for email: {}", signUpRequest.getEmail());
        
        // Check if email is already taken
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("Email already in use: {}", signUpRequest.getEmail());
            throw new BadRequestException("Email is already taken!");
        }

        try {
            // Create user's account
            User user = new User();
            user.setName(signUpRequest.getName());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            user.setProvider("local");

            Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleType.ROLE_USER));

            user.setRoles(Collections.singleton(userRole));

            User result = userRepository.save(user);
            log.info("User registered successfully: {}", result.getEmail());

            URI location = ServletUriComponentsBuilder
                    .fromCurrentContextPath().path("/api/users/{id}")
                    .buildAndExpand(result.getId()).toUri();

            // Return JSON response
            return ResponseEntity.created(location)
                    .body(new ApiResponse(true, "User registered successfully"));
        } catch (ResourceNotFoundException e) {
            log.error("Required role not found", e);
            throw e;
        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw new BadRequestException("Error during registration: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logoutUser() {
        log.info("Processing logout request");
        
        // For JWT-based authentication, logout is handled client-side
        // by removing the token from local storage
        return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
    }
} 