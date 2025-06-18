package com.codemate.controller;

import com.codemate.model.Pet;
import com.codemate.model.PetType;
import com.codemate.payload.ApiResponse;
import com.codemate.payload.DataResponse;
import com.codemate.payload.request.AdminUpdatePetRequest;
import com.codemate.payload.response.PetResponse;
import com.codemate.service.AdminPetService;
import com.codemate.service.PetService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/pets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPetController {
    
    private final AdminPetService adminPetService;
    
    public AdminPetController(AdminPetService adminPetService) {
        this.adminPetService = adminPetService;
    }
    
    @GetMapping
    public ResponseEntity<DataResponse<List<PetResponse>>> getAllPets() {
        
        log.debug("Admin getting all pets");
        
        List<Pet> pets = adminPetService.getAllPets();
        List<PetResponse> response = pets.stream()
                .map(this::convertToPetResponseWithOwner)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/{petId}")
    public ResponseEntity<DataResponse<PetResponse>> getPetById(@PathVariable Long petId) {
        
        log.debug("Admin getting pet by ID: {}", petId);
        
        Pet pet = adminPetService.getPetById(petId);
        PetResponse response = convertToPetResponseWithOwner(pet);
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/type/{type}")
    public ResponseEntity<DataResponse<List<PetResponse>>> getPetsByType(@PathVariable PetType type) {
        
        log.debug("Admin getting pets by type: {}", type);
        
        List<Pet> pets = adminPetService.getPetsByType(type);
        List<PetResponse> response = pets.stream()
                .map(this::convertToPetResponseWithOwner)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @GetMapping("/low-happiness")
    public ResponseEntity<DataResponse<List<PetResponse>>> getPetsWithLowHappiness(
            @RequestParam(defaultValue = "30") int threshold) {
        
        log.debug("Admin getting pets with happiness below: {}", threshold);
        
        List<Pet> pets = adminPetService.getPetsWithLowHappiness(threshold);
        List<PetResponse> response = pets.stream()
                .map(this::convertToPetResponseWithOwner)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @PutMapping("/{petId}")
    public ResponseEntity<DataResponse<PetResponse>> updatePet(
            @PathVariable Long petId,
            @Valid @RequestBody AdminUpdatePetRequest request) {
        
        log.debug("Admin updating pet ID: {} with data: {}", petId, request);
        
        Pet pet = adminPetService.getPetById(petId); // Get current pet
        
        // Update name if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            pet = adminPetService.updatePetName(petId, request.getName());
        }
        
        // Update happiness if provided
        if (request.getHappiness() != null) {
            pet = adminPetService.updatePetHappiness(petId, request.getHappiness());
        }
        
        PetResponse response = convertToPetResponseWithOwner(pet);
        
        return ResponseEntity.ok(DataResponse.success("Pet updated successfully", response));
    }
    
    @PostMapping("/{petId}/feed")
    public ResponseEntity<DataResponse<PetResponse>> forceFeedPet(@PathVariable Long petId) {
        
        log.debug("Admin force feeding pet ID: {}", petId);
        
        Pet pet = adminPetService.forceFeedPet(petId);
        PetResponse response = convertToPetResponseWithOwner(pet);
        
        return ResponseEntity.ok(DataResponse.success("Pet fed successfully", response));
    }
    
    @DeleteMapping("/{petId}")
    public ResponseEntity<ApiResponse> deletePet(@PathVariable Long petId) {
        
        log.debug("Admin deleting pet ID: {}", petId);
        
        adminPetService.deletePet(petId);
        
        return ResponseEntity.ok(new ApiResponse(true, "Pet deleted successfully"));
    }
    
    @GetMapping("/stats")
    public ResponseEntity<DataResponse<PetService.PetStats>> getPetStatistics() {
        
        log.debug("Admin getting pet statistics");
        
        PetService.PetStats stats = adminPetService.getPetStatistics();
        
        return ResponseEntity.ok(DataResponse.success(stats));
    }
    
    // Helper method
    private PetResponse convertToPetResponseWithOwner(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getType(),
                pet.getHappiness(),
                pet.getTotalPointsEarned(),
                pet.getLastFed(),
                pet.getCreatedAt(),
                pet.getOwner().getId(),
                pet.getOwner().getName(),
                pet.getOwner().getEmail()
        );
    }
} 