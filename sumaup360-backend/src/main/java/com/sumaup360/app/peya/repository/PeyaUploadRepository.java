package com.sumaup360.app.peya.repository;

import com.sumaup360.app.peya.domain.PeyaUpload;
import com.sumaup360.app.peya.enums.PeyaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PeyaUploadRepository extends JpaRepository<PeyaUpload, UUID> {
    List<PeyaUpload> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long countByUserId(UUID userId);
    List<PeyaUpload> findAllByOrderByCreatedAtDesc();
    List<PeyaUpload> findByEstadoOrderByCreatedAtDesc(PeyaStatus estado);
}
