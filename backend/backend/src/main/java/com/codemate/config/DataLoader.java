package com.codemate.config;

import com.codemate.model.Role;
import com.codemate.model.RoleType;
import com.codemate.model.User;
import com.codemate.repository.RoleRepository;
import com.codemate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default roles if they don't exist
        createRoleIfNotExists(RoleType.ROLE_USER);
        createRoleIfNotExists(RoleType.ROLE_ADMIN);
        
        // Create test users if they don't exist
        createTestUserIfNotExists();
        
        System.out.println("=================================");
        System.out.println("DataLoader: Initialization complete");
        System.out.println("Test user credentials:");
        System.out.println("Email: admin@codemate.com");
        System.out.println("Password: password123");
        System.out.println("=================================");
    }

    /**
     * Creates a role if it doesn't already exist in the database
     * 
     * @param roleType - The type of role to create (ROLE_USER, ROLE_ADMIN, etc.)
     */
    private void createRoleIfNotExists(RoleType roleType) {
        if (!roleRepository.findByName(roleType).isPresent()) {
            Role role = new Role();
            role.setName(roleType);
            roleRepository.save(role);
            System.out.println("Created role: " + roleType);
        }
    }

    /**
     * Creates test users for development and testing purposes
     * These users can be used to test the login functionality
     */
    private void createTestUserIfNotExists() {
        // Create admin user
        if (!userRepository.existsByEmail("admin@codemate.com")) {
            User adminUser = new User();
            adminUser.setName("Admin User");
            adminUser.setEmail("admin@codemate.com");
            adminUser.setPassword(passwordEncoder.encode("password123"));
            adminUser.setProvider("local");
            adminUser.setEnabled(true);
            
            // Assign ADMIN role
            Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Admin Role not found"));
            adminUser.setRoles(Collections.singleton(adminRole));
            
            userRepository.save(adminUser);
            System.out.println("Created admin user: admin@codemate.com");
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
            Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("User Role not found"));
            regularUser.setRoles(Collections.singleton(userRole));
            
            userRepository.save(regularUser);
            System.out.println("Created regular user: user@codemate.com");
        }
    }
} 