package com.codemate.config;

import com.codemate.exception.DataInitializationException;
import com.codemate.model.Role;
import com.codemate.model.RoleType;
import com.codemate.model.User;
import com.codemate.repository.RoleRepository;
import com.codemate.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    
    @Value("${app.default.admin.password:#{null}}")
    private String defaultAdminPassword;
    
    @Value("${app.default.user.password:#{null}}")
    private String defaultUserPassword;
    
    @Value("${app.data-loader.create-default-users:true}")
    private boolean createDefaultUsers;
    
    @Value("${app.data-loader.log-credentials:false}")
    private boolean logCredentials;

    public DataLoader(UserRepository userRepository, 
                     RoleRepository roleRepository, 
                     PasswordEncoder passwordEncoder,
                     Environment environment) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            validateConfiguration();
            
            // Create default roles if they don't exist
            createRoleIfNotExists(RoleType.ROLE_USER);
            createRoleIfNotExists(RoleType.ROLE_ADMIN);
            
            // Create test users only if configured to do so
            if (createDefaultUsers) {
                createDefaultUsersIfNotExists();
            }
            
            log.info("DataLoader: Initialization complete. Create users enabled: {}, Environment: {}", 
                    createDefaultUsers, getActiveProfilesAsString());
            if (logCredentials && isDevelopmentEnvironment()) {
                log.warn("Default user credentials are available - check application configuration");
            }
            logInitializationSummary();
        } catch (DataAccessException e) {
            log.error("Database error during data initialization: {}", e.getMessage(), e);
            throw new DataInitializationException("Failed to initialize data due to database error", e);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("Configuration error during data initialization: {}", e.getMessage());
            throw new DataInitializationException("Invalid configuration: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during data initialization: {}", e.getMessage(), e);
            throw new DataInitializationException("Data initialization failed", e);
        }
    }

    /**
     * Creates a role if it doesn't already exist in the database
     * 
     * @param roleType - The type of role to create (ROLE_USER, ROLE_ADMIN, etc.)
     * @throws DataInitializationException if role creation fails
     */
    private void createRoleIfNotExists(RoleType roleType) {
        try {
            if (roleRepository.findByName(roleType).isEmpty()) {
                Role role = new Role();
                role.setName(roleType);
                roleRepository.save(role);
                log.info("Created role: {}", roleType);
            } else {
                log.debug("Role already exists: {}", roleType);
            }
        } catch (DataAccessException e) {
            throw new DataInitializationException("Failed to create role: " + roleType, e);
        }
    }

    @Transactional
    private void createDefaultUsersIfNotExists() {
        Role adminRole = roleRepository.findByName(RoleType.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Admin Role not found"));
        Role userRole = roleRepository.findByName(RoleType.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("User Role not found"));
        
        createAdminUserIfNotExists(adminRole, userRole);
        createRegularUserIfNotExists(userRole);
    }
    
    @Transactional
    private void createAdminUserIfNotExists(Role adminRole, Role userRole) {
        final String adminEmail = "admin@codemate.com";
        
        if (!StringUtils.hasText(defaultAdminPassword)) {
            log.warn("No admin password configured. Skipping admin user creation. " +
                    "Set app.default.admin.password to create default admin user.");
            return;
        }
        
        try {
            User adminUser = userRepository.findByEmail(adminEmail).orElse(null);
            
            if (adminUser == null) {
                adminUser = createUser("Admin User", adminEmail, defaultAdminPassword, "local");
                adminUser.getRoles().add(adminRole);
                adminUser.getRoles().add(userRole);
                userRepository.save(adminUser);
                log.info("Created admin user: {} with roles: [ADMIN, USER]", adminEmail);
            } else {
                ensureUserHasRoles(adminUser, adminRole, userRole);
            }
        } catch (DataAccessException e) {
            throw new DataInitializationException("Failed to create admin user", e);
        }
    }
    
    @Transactional
    private void createRegularUserIfNotExists(Role userRole) {
        final String userEmail = "user@codemate.com";
        
        if (!StringUtils.hasText(defaultUserPassword)) {
            log.warn("No user password configured. Skipping regular user creation. " +
                    "Set app.default.user.password to create default user.");
            return;
        }
        
        try {
            if (!userRepository.existsByEmail(userEmail)) {
                User regularUser = createUser("Test User", userEmail, defaultUserPassword, "local");
                regularUser.setRoles(Collections.singleton(userRole));
                userRepository.save(regularUser);
                log.info("Created regular user: {} with roles: [USER]", userEmail);
            } else {
                log.debug("Regular user already exists: {}", userEmail);
            }
        } catch (DataAccessException e) {
            throw new DataInitializationException("Failed to create regular user", e);
        }
    }
    
    private User createUser(String name, String email, String password, String provider) {
        validateUserInput(name, email, password, provider);
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setProvider(provider);
        user.setEnabled(true);
        return user;
    }
    
    private void validateUserInput(String name, String email, String password, String provider) {
        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("User email cannot be empty");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("User password cannot be empty");
        }
        if (!StringUtils.hasText(provider)) {
            throw new IllegalArgumentException("User provider cannot be empty");
        }
        
        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }
    
    @Transactional
    private void ensureUserHasRoles(User user, Role... requiredRoles) {
        try {
            boolean needsUpdate = false;
            
            for (Role requiredRole : requiredRoles) {
                boolean hasRole = user.getRoles().stream()
                        .anyMatch(role -> role.getName() == requiredRole.getName());
                
                if (!hasRole) {
                    user.getRoles().add(requiredRole);
                    needsUpdate = true;
                }
            }
            
            if (needsUpdate) {
                userRepository.save(user);
                log.info("Updated user roles for: {}", user.getEmail());
            }
        } catch (DataAccessException e) {
            throw new DataInitializationException("Failed to update user roles for: " + user.getEmail(), e);
        }
    }
    
    private void validateConfiguration() {
        if (createDefaultUsers) {
            if (!StringUtils.hasText(defaultAdminPassword) && !StringUtils.hasText(defaultUserPassword)) {
                log.warn("Default user creation is enabled but no passwords are configured. " +
                        "Users will not be created unless passwords are provided.");
            }
            
            if (isProductionEnvironment() && 
                (isWeakPassword(defaultAdminPassword) || isWeakPassword(defaultUserPassword))) {
                throw new IllegalStateException(
                    "Weak default passwords detected in production environment. " +
                    "Please configure strong passwords or disable default user creation.");
            }
        }
    }
    
    private boolean isDevelopmentEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 || 
               java.util.Arrays.asList(activeProfiles).contains("dev") ||
               java.util.Arrays.asList(activeProfiles).contains("development") ||
               java.util.Arrays.asList(activeProfiles).contains("local");
    }
    
    private boolean isProductionEnvironment() {
        String[] activeProfiles = environment.getActiveProfiles();
        return java.util.Arrays.asList(activeProfiles).contains("prod") ||
               java.util.Arrays.asList(activeProfiles).contains("production");
    }
    
    private boolean isWeakPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false; // null/empty passwords are handled elsewhere
        }
        
        return password.length() < 8 || 
               password.equals("password") ||
               password.equals("password123") ||
               password.equals("admin") ||
               password.equals("test");
    }
    
    private String getActiveProfilesAsString() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? String.join(", ", profiles) : "default";
    }
    
    private void logInitializationSummary() {
        try {
            long roleCount = roleRepository.count();
            long userCount = userRepository.count();
            
            log.info("Data initialization summary - Roles: {}, Users: {}, Environment: {}", 
                    roleCount, userCount, getActiveProfilesAsString());
                    
            if (isDevelopmentEnvironment()) {
                log.debug("Development environment detected - additional logging enabled");
            }
            
            if (isProductionEnvironment()) {
                log.info("Production environment detected - security validations applied");
            }
        } catch (Exception e) {
            log.warn("Failed to generate initialization summary: {}", e.getMessage());
        }
    }
} 