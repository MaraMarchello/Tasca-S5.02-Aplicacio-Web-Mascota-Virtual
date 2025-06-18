package com.codemate.repository;

import com.codemate.model.Pet;
import com.codemate.model.PetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    
    // Find pet by owner ID
    Optional<Pet> findByOwnerId(Long ownerId);
    
    // Check if user already has a pet
    boolean existsByOwnerId(Long ownerId);
    
    // Find pets by type
    List<Pet> findByType(PetType type);
    
    // Find pets with low happiness (for admin monitoring)
    @Query("SELECT p FROM Pet p WHERE p.happiness < :threshold")
    List<Pet> findByHappinessLessThan(@Param("threshold") Integer threshold);
    
    // Find pets that haven't been fed recently (for admin monitoring)
    @Query("SELECT p FROM Pet p WHERE p.lastFed < :date")
    List<Pet> findByLastFedBefore(@Param("date") Date date);
    
    // Count pets by type (for statistics)
    long countByType(PetType type);
    
    // Get pets with owner information for admin
    @Query("SELECT p FROM Pet p JOIN FETCH p.owner")
    List<Pet> findAllWithOwners();
    
    // Count happy pets (for admin stats)
    long countByHappinessGreaterThan(int happiness);
} 