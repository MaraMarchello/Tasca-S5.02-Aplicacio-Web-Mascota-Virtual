package com.codemate.controller;

import com.codemate.model.Pet;
import com.codemate.payload.DataResponse;
import com.codemate.payload.request.CreatePetRequest;
import com.codemate.payload.request.UpdatePetNameRequest;
import com.codemate.payload.response.PetResponse;
import com.codemate.security.CurrentUser;
import com.codemate.security.UserPrincipal;
import com.codemate.service.PetService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/pets")
public class PetController {
    
    private final PetService petService;
    
    public PetController(PetService petService) {
        this.petService = petService;
    }
    
    @PostMapping
    public ResponseEntity<DataResponse<PetResponse>> createPet(
            @Valid @RequestBody CreatePetRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Creating pet for user ID: {}, pet name: {}, type: {}", 
                userPrincipal.getId(), request.getName(), request.getType());
        
        Pet pet = petService.createPet(userPrincipal.getId(), request.getName(), request.getType());
        PetResponse response = convertToPetResponse(pet);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DataResponse.success("Pet created successfully", response));
    }
    
    @GetMapping("/my-pet")
    public ResponseEntity<DataResponse<PetResponse>> getMyPet(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Getting pet for user ID: {}", userPrincipal.getId());
        
        Optional<Pet> petOpt = petService.getPetByUserId(userPrincipal.getId());
        if (petOpt.isEmpty()) {
            return ResponseEntity.ok(DataResponse.success("No pet found", null));
        }
        
        PetResponse response = convertToPetResponse(petOpt.get());
        return ResponseEntity.ok(DataResponse.success(response));
    }
    
    @PostMapping("/my-pet/feed")
    public ResponseEntity<DataResponse<PetResponse>> feedMyPet(@CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Feeding pet for user ID: {}", userPrincipal.getId());
        
        Pet pet = petService.feedPet(userPrincipal.getId());
        PetResponse response = convertToPetResponse(pet);
        
        return ResponseEntity.ok(DataResponse.success("Pet fed successfully", response));
    }
    
    @PutMapping("/my-pet/name")
    public ResponseEntity<DataResponse<PetResponse>> updatePetName(
            @Valid @RequestBody UpdatePetNameRequest request,
            @CurrentUser UserPrincipal userPrincipal) {
        
        log.debug("Updating pet name for user ID: {}, new name: {}", 
                userPrincipal.getId(), request.getName());
        
        Pet pet = petService.updatePetName(userPrincipal.getId(), request.getName());
        PetResponse response = convertToPetResponse(pet);
        
        return ResponseEntity.ok(DataResponse.success("Pet name updated successfully", response));
    }
    
    @GetMapping("/types")
    public ResponseEntity<DataResponse<Object[]>> getAvailablePetTypes() {
        
        log.debug("Getting available pet types");
        
        return ResponseEntity.ok(DataResponse.success(com.codemate.model.PetType.values()));
    }
    
    // Helper method to convert Pet entity to PetResponse
    private PetResponse convertToPetResponse(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getType(),
                pet.getHappiness(),
                pet.getTotalPointsEarned(),
                pet.getLastFed(),
                pet.getCreatedAt()
        );
    }
} 