package com.sumaup360.app.honorarios.repository;

import com.sumaup360.app.honorarios.domain.HonorarioRequest;
import com.sumaup360.app.honorarios.enums.HonorarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HonorarioRequestRepository extends JpaRepository<HonorarioRequest, UUID> {

    List<HonorarioRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserId(UUID userId);

    Optional<HonorarioRequest> findByIdAndUserId(UUID id, UUID userId);

    List<HonorarioRequest> findAllByOrderByCreatedAtDesc();

    List<HonorarioRequest> findByEstadoOrderByCreatedAtDesc(HonorarioStatus estado);
}
