package com.codemate.controller;

import com.codemate.model.User;
import com.codemate.payload.ApiResponse;
import com.codemate.payload.DataResponse;
import com.codemate.payload.request.AdminGrantPointsRequest;
import com.codemate.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
    
    private final AdminUserService adminUserService;
    
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }
    
    @GetMapping
    public ResponseEntity<DataResponse<List<User>>> getAllUsers() {
        
        log.debug("Admin getting all users");
        
        List<User> users = adminUserService.getAllUsers();
        
        return ResponseEntity.ok(DataResponse.success(users));
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<DataResponse<User>> getUserById(@PathVariable Long userId) {
        
        log.debug("Admin getting user by ID: {}", userId);
        
        User user = adminUserService.getUserById(userId);
        
        return ResponseEntity.ok(DataResponse.success(user));
    }
    
    @GetMapping("/email/{email}")
    public ResponseEntity<DataResponse<User>> getUserByEmail(@PathVariable String email) {
        
        log.debug("Admin getting user by email: {}", email);
        
        User user = adminUserService.getUserByEmail(email);
        
        return ResponseEntity.ok(DataResponse.success(user));
    }
    
    @GetMapping("/{userId}/points/balance")
    public ResponseEntity<DataResponse<Long>> getUserPointBalance(@PathVariable Long userId) {
        
        log.debug("Admin getting point balance for user: {}", userId);
        
        Long balance = adminUserService.getUserPointBalance(userId);
        
        return ResponseEntity.ok(DataResponse.success(balance));
    }
    
    @GetMapping("/{userId}/points/total-earned")
    public ResponseEntity<DataResponse<Long>> getUserTotalPointsEarned(@PathVariable Long userId) {
        
        log.debug("Admin getting total points earned for user: {}", userId);
        
        Long totalEarned = adminUserService.getUserTotalPointsEarned(userId);
        
        return ResponseEntity.ok(DataResponse.success(totalEarned));
    }
    
    @PostMapping("/grant-points")
    public ResponseEntity<ApiResponse> grantPointsToUser(@Valid @RequestBody AdminGrantPointsRequest request) {
        
        log.debug("Admin granting {} points to user {}: {}", 
                request.getAmount(), request.getUserId(), request.getReason());
        
        adminUserService.grantPointsToUser(request.getUserId(), request.getAmount(), request.getReason());
        
        return ResponseEntity.ok(new ApiResponse(true, 
                String.format("Successfully granted %d points to user", request.getAmount())));
    }
    
    @PutMapping("/{userId}/status")
    public ResponseEntity<ApiResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> statusUpdate) {
        
        log.debug("Admin updating status for user ID: {}", userId);
        
        boolean enabled = statusUpdate.get("enabled");
        adminUserService.updateUserStatus(userId, enabled);
        
        return ResponseEntity.ok(new ApiResponse(true, "User status updated successfully"));
    }
    
    @GetMapping("/{userId}/stats")
    public ResponseEntity<DataResponse<AdminUserService.UserStats>> getUserStats(@PathVariable Long userId) {
        
        log.debug("Admin getting statistics for user: {}", userId);
        
        AdminUserService.UserStats stats = adminUserService.getUserStats(userId);
        
        return ResponseEntity.ok(DataResponse.success(stats));
    }
    
    @GetMapping("/system-stats")
    public ResponseEntity<DataResponse<AdminUserService.SystemStats>> getSystemStats() {
        
        log.debug("Admin getting system statistics");
        
        AdminUserService.SystemStats stats = adminUserService.getSystemStats();
        
        return ResponseEntity.ok(DataResponse.success(stats));
    }
} 