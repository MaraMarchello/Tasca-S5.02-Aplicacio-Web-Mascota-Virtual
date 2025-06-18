package com.codemate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PetAlreadyExistsException extends RuntimeException {
    
    public PetAlreadyExistsException(String message) {
        super(message);
    }
    
    public PetAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static PetAlreadyExistsException forUser(Long userId) {
        return new PetAlreadyExistsException("User with id " + userId + " already has a pet. Only one pet per user is allowed.");
    }
} 