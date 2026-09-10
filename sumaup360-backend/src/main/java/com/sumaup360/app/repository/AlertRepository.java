package com.sumaup360.app.repository;

import com.sumaup360.app.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {
    List<Alert> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Alert> findByIdAndUserId(UUID id, UUID userId);
}
