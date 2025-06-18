package com.codemate.service;

import com.codemate.exception.ResourceNotFoundException;
import com.codemate.model.Pet;
import com.codemate.model.PetType;
import com.codemate.repository.PetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class AdminPetService {
    
    private final PetRepository petRepository;
    private final PetService petService;
    
    public AdminPetService(PetRepository petRepository, PetService petService) {
        this.petRepository = petRepository;
        this.petService = petService;
    }
    
    /**
     * Get all pets with owner information
     */
    @Transactional(readOnly = true)
    public List<Pet> getAllPets() {
        return petRepository.findAllWithOwners();
    }
    
    /**
     * Get pet by ID (admin can access any pet)
     */
    @Transactional(readOnly = true)
    public Pet getPetById(Long petId) {
        return petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
    }
    
    /**
     * Get pets by type
     */
    @Transactional(readOnly = true)
    public List<Pet> getPetsByType(PetType type) {
        return petRepository.findByType(type);
    }
    
    /**
     * Get pets with low happiness (for monitoring)
     */
    @Transactional(readOnly = true)
    public List<Pet> getPetsWithLowHappiness(int threshold) {
        return petRepository.findByHappinessLessThan(threshold);
    }
    
    /**
     * Get pets that haven't been fed recently
     */
    @Transactional(readOnly = true)
    public List<Pet> getPetsNotFedRecently(Date beforeDate) {
        return petRepository.findByLastFedBefore(beforeDate);
    }
    
    /**
     * Update pet happiness (admin override)
     */
    public Pet updatePetHappiness(Long petId, int newHappiness) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        
        // Ensure happiness is within valid range
        int validHappiness = Math.max(0, Math.min(100, newHappiness));
        pet.setHappiness(validHappiness);
        
        return petRepository.save(pet);
    }
    
    /**
     * Update pet name (admin override)
     */
    public Pet updatePetName(Long petId, String newName) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        
        if (newName == null || newName.trim().isEmpty() || newName.length() > 50) {
            throw new IllegalArgumentException("Pet name must be between 1 and 50 characters");
        }
        
        pet.setName(newName.trim());
        return petRepository.save(pet);
    }
    
    /**
     * Delete pet (admin action)
     */
    public void deletePet(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResourceNotFoundException("Pet", "id", petId);
        }
        petRepository.deleteById(petId);
    }
    
    /**
     * Get pet statistics
     */
    @Transactional(readOnly = true)
    public PetService.PetStats getPetStatistics() {
        return petService.getPetStats();
    }
    
    /**
     * Force feed pet (admin action)
     */
    public Pet forceFeedPet(Long petId) {
        Pet pet = petRepository.findById(petId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
        
        int newHappiness = Math.min(100, pet.getHappiness() + 20);
        pet.setHappiness(newHappiness);
        pet.setLastFed(new Date());
        
        return petRepository.save(pet);
    }
} 