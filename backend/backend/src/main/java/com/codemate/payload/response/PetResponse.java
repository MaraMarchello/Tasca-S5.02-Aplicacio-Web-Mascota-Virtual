package com.codemate.payload.response;

import com.codemate.model.PetType;
import lombok.Data;

import java.util.Date;

@Data
public class PetResponse {
    private Long id;
    private String name;
    private PetType type;
    private Integer happiness;
    private Long totalPointsEarned;
    private Date lastFed;
    private Date createdAt;
    
    // User information for admin views
    private UserInfo user;
    
    // Constructor for user view (without owner info)
    public PetResponse(Long id, String name, PetType type, Integer happiness, 
                      Long totalPointsEarned, Date lastFed, Date createdAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.happiness = happiness;
        this.totalPointsEarned = totalPointsEarned;
        this.lastFed = lastFed;
        this.createdAt = createdAt;
    }
    
    // Constructor for admin view (with owner info)
    public PetResponse(Long id, String name, PetType type, Integer happiness, 
                      Long totalPointsEarned, Date lastFed, Date createdAt, 
                      Long userId, String userName, String userEmail) {
        this(id, name, type, happiness, totalPointsEarned, lastFed, createdAt);
        this.user = new UserInfo(userId, userName, userEmail);
    }
    
    // Default constructor for Lombok
    public PetResponse() {}
    
    // Inner class for user information
    @Data
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
        
        public UserInfo(Long id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        public UserInfo() {}
    }
} 