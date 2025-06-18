package com.codemate.controller;

import com.codemate.model.PointTransaction;
import com.codemate.payload.ApiResponse;
import com.codemate.service.AdminService;
import com.codemate.service.PetService;
import com.codemate.service.PointTransactionService;
import com.codemate.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private UserService userService;

    @Autowired
    private PetService petService;

    @Autowired
    private PointTransactionService pointTransactionService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse> getAdminStats() {
        Map<String, Object> stats = adminService.getAdminStats();
        return ResponseEntity.ok(new ApiResponse(true, "Admin stats retrieved successfully", stats));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse> getTransactions(@RequestParam(defaultValue = "50") int limit) {
        List<PointTransaction> transactions = pointTransactionService.getRecentTransactions(limit);
        return ResponseEntity.ok(new ApiResponse(true, "Transactions retrieved successfully", transactions));
    }

    @PostMapping("/points/award")
    public ResponseEntity<ApiResponse> awardPoints(@RequestBody Map<String, Object> awardRequest) {
        Long userId = Long.valueOf(awardRequest.get("userId").toString());
        Integer amount = Integer.valueOf(awardRequest.get("amount").toString());
        String description = awardRequest.get("description").toString();
        
        pointTransactionService.awardPointsByAdmin(userId, amount, description);
        return ResponseEntity.ok(new ApiResponse(true, "Points awarded successfully"));
    }
} 