package com.sumaup360.app.honorarios.repository;

import com.sumaup360.app.honorarios.domain.SuspensionRequest;
import com.sumaup360.app.honorarios.enums.SuspensionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuspensionRequestRepository extends JpaRepository<SuspensionRequest, UUID> {

    List<SuspensionRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserId(UUID userId);

    Optional<SuspensionRequest> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndAnio(UUID userId, int anio);

    List<SuspensionRequest> findAllByOrderByCreatedAtDesc();

    List<SuspensionRequest> findByEstadoOrderByCreatedAtDesc(SuspensionStatus estado);
}
