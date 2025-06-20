package com.codemate.config;

import com.codemate.model.Role;
import com.codemate.model.RoleType;
import com.codemate.model.User;
import com.codemate.repository.RoleRepository;
import com.codemate.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, 
                     RoleRepository roleRepository, 
                     PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create default roles if they don't exist
        createRoleIfNotExists(RoleType.ROLE_USER);
        createRoleIfNotExists(RoleType.ROLE_ADMIN);
        
        // Create test users if they don't exist
        createTestUserIfNotExists();
        
        log.info("=================================");
        log.info("DataLoader: Initialization complete");
        log.info("Test user credentials:");
        log.info("Email: admin@codemate.com");
        log.info("Password: password123");
        log.info("=================================");
    }

    /**
     * Creates a role if it doesn't already exist in the database
     * 
     * @param roleType - The type of role to create (ROLE_USER, ROLE_ADMIN, etc.)
     */
    private void createRoleIfNotExists(RoleType roleType) {
        if (roleRepository.findByName(roleType).isEmpty()) {
            Role role = new Role();
            role.setName(roleType);
            roleRepository.save(role);
            log.info("Created role: {}", roleType);
        }
    }

    /**
     * Creates test users for development and testing purposes
     * These users can be used to test the login functionality
     */
    private void createTestUserIfNotExists() {
        // Create or fix admin user
        User adminUser = userRepository.findByEmail("admin@codemate.com").orElse(null);
        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new RuntimeException("Admin Role not found"));
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("User Role not found"));
        
        if (adminUser == null) {
            // Create new admin user with both ADMIN and USER roles
            adminUser = new User();
            adminUser.setName("Admin User");
            adminUser.setEmail("admin@codemate.com");
            adminUser.setPassword(passwordEncoder.encode("password123"));
            adminUser.setProvider("local");
            adminUser.setEnabled(true);
            // Admin users should have both ADMIN and USER roles
            adminUser.getRoles().add(adminRole);
            adminUser.getRoles().add(userRole);
            userRepository.save(adminUser);
            log.info("Created admin user: admin@codemate.com with ADMIN and USER roles");
        } else {
            // Check if admin user has both required roles, if not, fix it
            boolean hasAdminRole = adminUser.getRoles().stream()
                    .anyMatch(role -> role.getName() == RoleType.ROLE_ADMIN);
            boolean hasUserRole = adminUser.getRoles().stream()
                    .anyMatch(role -> role.getName() == RoleType.ROLE_USER);
            
            if (!hasAdminRole || !hasUserRole) {
                log.warn("Admin user exists but doesn't have required roles. Fixing...");
                adminUser.getRoles().clear(); // Clear existing roles
                adminUser.getRoles().add(adminRole); // Add ADMIN role
                adminUser.getRoles().add(userRole);  // Add USER role
                userRepository.save(adminUser);
                log.info("Fixed admin user roles: admin@codemate.com now has both ADMIN and USER roles");
            }
        }

        // Create regular user
        if (!userRepository.existsByEmail("user@codemate.com")) {
            User regularUser = new User();
            regularUser.setName("Test User");
            regularUser.setEmail("user@codemate.com");
            regularUser.setPassword(passwordEncoder.encode("password123"));
            regularUser.setProvider("local");
            regularUser.setEnabled(true);
            
            // Assign USER role
            regularUser.setRoles(Collections.singleton(userRole));
            
            userRepository.save(regularUser);
            log.info("Created regular user: user@codemate.com");
        }
    }
} 