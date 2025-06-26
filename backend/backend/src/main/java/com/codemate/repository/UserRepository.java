package com.codemate.repository;

import com.codemate.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    Boolean existsByEmail(String email);
    Boolean existsByName(String name);
    
    // Admin methods
    long countByEnabledTrue();
    long countByCreatedAtAfter(LocalDateTime date);
    
    // Analytics methods
    default long countActiveUsers() {
        return countByEnabledTrue();
    }
} 