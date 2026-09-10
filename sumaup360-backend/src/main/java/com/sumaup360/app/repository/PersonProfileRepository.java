package com.sumaup360.app.repository;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.enums.ClientType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonProfileRepository extends JpaRepository<PersonProfile, UUID> {
    Optional<PersonProfile> findByUserId(UUID userId);

    // --- Resolucion de audiencias para campanas y recordatorios de push ---

    @Query("select p.userId from PersonProfile p")
    List<UUID> findAllUserIds();

    @Query("select p.userId from PersonProfile p where p.clientType = :clientType")
    List<UUID> findUserIdsByClientType(@Param("clientType") ClientType clientType);

    @Query("select p.userId from PersonProfile p where p.onboardingCompleted = false")
    List<UUID> findUserIdsWithOnboardingIncomplete();

    @Query("select p.userId from PersonProfile p where p.orientationStatus = :status")
    List<UUID> findUserIdsByOrientationStatus(@Param("status") String status);
}
