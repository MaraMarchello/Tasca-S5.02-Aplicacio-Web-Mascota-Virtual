package com.codemate.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DataResponse<T> {
    private boolean success;
    private String message;
    private T data;
    
    public DataResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    public static <T> DataResponse<T> success(T data) {
        return new DataResponse<>(true, "Success", data);
    }
    
    public static <T> DataResponse<T> success(String message, T data) {
        return new DataResponse<>(true, message, data);
    }
    
    public static <T> DataResponse<T> error(String message) {
        return new DataResponse<>(false, message, null);
    }
} 