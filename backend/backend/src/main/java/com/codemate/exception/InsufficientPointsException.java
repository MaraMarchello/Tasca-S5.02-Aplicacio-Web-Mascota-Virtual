package com.codemate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientPointsException extends RuntimeException {
    
    public InsufficientPointsException(String message) {
        super(message);
    }
    
    public InsufficientPointsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static InsufficientPointsException forPurchase(Long required, Long available) {
        return new InsufficientPointsException(
            String.format("Insufficient points for purchase. Required: %d, Available: %d", required, available)
        );
    }
} 