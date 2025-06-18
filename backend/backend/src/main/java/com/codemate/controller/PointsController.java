package com.codemate.controller;

import com.codemate.model.PointTransaction;
import com.codemate.payload.DataResponse;
import com.codemate.payload.response.PointBalanceResponse;
import com.codemate.payload.response.PointTransactionResponse;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.PointTransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/points")
@PreAuthorize("hasRole('USER')")
public class PointsController {
    
    private final PointTransactionService pointTransactionService;
    
    public PointsController(PointTransactionService pointTransactionService) {
        this.pointTransactionService = pointTransactionService;
    }
    
    @GetMapping("/balance")
    public ResponseEntity<DataResponse<PointBalanceResponse>> getPointBalance(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting point balance for user: {}", userPrincipal.getId());
        
        Long currentPoints = pointTransactionService.getCurrentPoints(userPrincipal.getId());
        Long totalEarned = pointTransactionService.getTotalPointsEarned(userPrincipal.getId());
        Long totalSpent = pointTransactionService.getTotalPointsSpent(userPrincipal.getId());
        
        PointBalanceResponse response = new PointBalanceResponse(currentPoints, totalEarned, totalSpent);
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/transactions")
    public ResponseEntity<DataResponse<List<PointTransactionResponse>>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting transaction history for user: {}, page: {}, size: {}", 
                userPrincipal.getId(), page, size);
        
        List<PointTransaction> transactions = pointTransactionService.getTransactionHistory(
                userPrincipal.getId(), page, size);
        
        List<PointTransactionResponse> response = transactions.stream()
                .map(this::convertToPointTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/transactions/recent")
    public ResponseEntity<DataResponse<List<PointTransactionResponse>>> getRecentTransactions(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting recent transactions for user: {}", userPrincipal.getId());
        
        List<PointTransaction> transactions = pointTransactionService.getRecentTransactions(userPrincipal.getId());
        
        List<PointTransactionResponse> response = transactions.stream()
                .map(this::convertToPointTransactionResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    // Helper method
    private PointTransactionResponse convertToPointTransactionResponse(PointTransaction transaction) {
        return new PointTransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getSource(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getReferenceId(),
                transaction.getCreatedAt()
        );
    }
} 