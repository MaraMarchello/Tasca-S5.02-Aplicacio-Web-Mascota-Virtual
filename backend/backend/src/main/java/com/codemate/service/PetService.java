package com.codemate.service;

import com.codemate.exception.BadRequestException;
import com.codemate.exception.PetAlreadyExistsException;
import com.codemate.exception.PetNotFoundException;
import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.Pet;
import com.codemate.model.PetType;
import com.codemate.model.User;
import com.codemate.repository.PetRepository;
import com.codemate.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class PetService {
    
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final PointTransactionService pointTransactionService;
    private final AchievementService achievementService;
    private static final Logger log = LoggerFactory.getLogger(PetService.class);
    
    public PetService(PetRepository petRepository,
                     UserRepository userRepository,
                     PointTransactionService pointTransactionService,
                     AchievementService achievementService) {
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.pointTransactionService = pointTransactionService;
        this.achievementService = achievementService;
    }
    
    /**
     * Create a new pet for a user
     */
    public Pet createPet(Long userId, String petName, PetType petType) {
        // Check if user already has a pet
        if (petRepository.existsByOwnerId(userId)) {
            throw PetAlreadyExistsException.forUser(userId);
        }
        
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        // Validate pet name
        if (petName == null || petName.trim().isEmpty() || petName.length() > 50) {
            throw new BadRequestException("Pet name must be between 1 and 50 characters");
        }
        
        Pet pet = new Pet();
        pet.setOwner(user);
        pet.setName(petName.trim());
        pet.setType(petType);
        pet.setHappiness(100); // Start with full happiness
        pet.setTotalPointsEarned(0L);
        pet.setLastFed(new Date());
        
        Pet savedPet = petRepository.save(pet);
        
        // Track achievement for creating a pet
        achievementService.trackPetCreation(userId);
        
        return savedPet;
    }
    
    /**
     * Get pet by user ID
     */
    @Transactional(readOnly = true)
    public Optional<Pet> getPetByUserId(Long userId) {
        return petRepository.findByOwnerId(userId);
    }
    
    /**
     * Get pet by ID (with ownership validation)
     */
    @Transactional(readOnly = true)
    public Pet getPetById(Long petId, Long userId) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        
        if (!pet.getOwner().getId().equals(userId)) {
            throw new BadRequestException("You can only access your own pet");
        }
        
        return pet;
    }
    
    /**
     * Feed the pet (increases happiness)
     */
    public Pet feedPet(Long userId) {
        Pet pet = getPetByUserId(userId)
            .orElseThrow(() -> PetNotFoundException.forUser(userId));
        
        // Update happiness (max 100)
        int newHappiness = Math.min(100, pet.getHappiness() + 20);
        pet.setHappiness(newHappiness);
        pet.setLastFed(new Date());
        
        Pet savedPet = petRepository.save(pet);
        
        // Track achievement for feeding pet
        achievementService.trackPetFeeding(userId);
        
        return savedPet;
    }
    
    /**
     * Update pet happiness (used by item consumption)
     */
    public Pet updateHappiness(Long petId, int happinessChange) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        
        int newHappiness = Math.max(0, Math.min(100, pet.getHappiness() + happinessChange));
        pet.setHappiness(newHappiness);
        
        return petRepository.save(pet);
    }
    
    /**
     * Update pet's total points earned (called when user earns points)
     */
    public void updateTotalPointsEarned(Long userId) {
        Optional<Pet> petOpt = getPetByUserId(userId);
        if (petOpt.isPresent()) {
            Pet pet = petOpt.get();
            Long totalPoints = pointTransactionService.getTotalPointsEarned(userId);
            pet.setTotalPointsEarned(totalPoints);
            petRepository.save(pet);
        }
    }
    
    /**
     * Update pet name
     */
    public Pet updatePetName(Long userId, String newName) {
        Pet pet = getPetByUserId(userId)
            .orElseThrow(() -> PetNotFoundException.forUser(userId));
        
        if (newName == null || newName.trim().isEmpty() || newName.length() > 50) {
            throw new BadRequestException("Pet name must be between 1 and 50 characters");
        }
        
        pet.setName(newName.trim());
        return petRepository.save(pet);
    }
    
    /**
     * Delete pet (for admin or user)
     */
    public void deletePet(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResourceNotFoundException("Pet", "id", petId);
        }
        petRepository.deleteById(petId);
    }
    
    /**
     * Delete user's own pet (with ownership validation)
     */
    public void deleteMyPet(Long userId) {
        Pet pet = getPetByUserId(userId)
            .orElseThrow(() -> PetNotFoundException.forUser(userId));
        
        // Additional validation to ensure user owns the pet
        if (!pet.getOwner().getId().equals(userId)) {
            throw new BadRequestException("You can only delete your own pet");
        }
        
        // Log the deletion for audit purposes
        log.info("User {} is deleting their pet: {} (ID: {})", 
                userId, pet.getName(), pet.getId());
        
        // Delete the pet (PetItems will be cascade deleted due to CascadeType.ALL)
        petRepository.delete(pet);
        
        log.info("Pet {} (ID: {}) successfully deleted for user {}", 
                pet.getName(), pet.getId(), userId);
    }
    
    /**
     * Get all pets (admin only)
     */
    @Transactional(readOnly = true)
    public List<Pet> getAllPets() {
        return petRepository.findAllWithOwners();
    }
    
    /**
     * Get pets by type (admin only)
     */
    @Transactional(readOnly = true)
    public List<Pet> getPetsByType(PetType type) {
        return petRepository.findByType(type);
    }
    
    /**
     * Get pets with low happiness (admin monitoring)
     */
    @Transactional(readOnly = true)
    public List<Pet> getPetsWithLowHappiness(int threshold) {
        return petRepository.findByHappinessLessThan(threshold);
    }
    
    /**
     * Get pet statistics
     */
    @Transactional(readOnly = true)
    public PetStats getPetStats() {
        long totalPets = petRepository.count();
        long dukeJavaPets = petRepository.countByType(PetType.DUKE_JAVA);
        long coffeeBeanPets = petRepository.countByType(PetType.COFFEE_BEAN);
        
        return new PetStats(totalPets, dukeJavaPets, coffeeBeanPets);
    }
    
    // Inner class for statistics
    public static class PetStats {
        private final long totalPets;
        private final long dukeJavaPets;
        private final long coffeeBeanPets;
        
        public PetStats(long totalPets, long dukeJavaPets, long coffeeBeanPets) {
            this.totalPets = totalPets;
            this.dukeJavaPets = dukeJavaPets;
            this.coffeeBeanPets = coffeeBeanPets;
        }
        
        public long getTotalPets() { return totalPets; }
        public long getDukeJavaPets() { return dukeJavaPets; }
        public long getCoffeeBeanPets() { return coffeeBeanPets; }
    }

    // Admin methods
    public List<Pet> getAllPetsWithUsers() {
        return petRepository.findAllWithOwners();
    }

    @Transactional
    public void deletePetByAdmin(Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException("Pet not found with id: " + petId));
        
        petRepository.delete(pet);
    }

    @Transactional
    public void updatePetHappinessByAdmin(Long petId, int happiness) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new PetNotFoundException("Pet not found with id: " + petId));
        
        if (happiness < 0 || happiness > 100) {
            throw new IllegalArgumentException("Happiness must be between 0 and 100");
        }
        
        pet.setHappiness(happiness);
        petRepository.save(pet);
    }
} 