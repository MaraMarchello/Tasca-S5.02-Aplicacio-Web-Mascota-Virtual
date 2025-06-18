package com.codemate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ItemNotFoundException extends RuntimeException {
    
    public ItemNotFoundException(String message) {
        super(message);
    }
    
    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static ItemNotFoundException forId(Long itemId) {
        return new ItemNotFoundException("Item not found with id: " + itemId);
    }
    
    public static ItemNotFoundException forUserItem(Long petItemId, Long userId) {
        return new ItemNotFoundException("Pet item with id " + petItemId + " not found for user " + userId);
    }
} 