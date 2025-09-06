package com.codemate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DataInitializationException extends RuntimeException {

    public DataInitializationException(String message) {
        super(message);
    }

    public DataInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
