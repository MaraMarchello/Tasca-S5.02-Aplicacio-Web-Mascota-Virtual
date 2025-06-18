package com.codemate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PetNotFoundException extends RuntimeException {
    
    public PetNotFoundException(String message) {
        super(message);
    }
    
    public PetNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PetNotFoundException forUser(Long userId) {
        return new PetNotFoundException("Pet not found for user with id: " + userId);
    }
    
    public static PetNotFoundException forId(Long petId) {
        return new PetNotFoundException("Pet not found with id: " + petId);
    }
} 