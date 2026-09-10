package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.CashSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashSessionRepository extends JpaRepository<CashSession, UUID> {
    Optional<CashSession> findByBranchIdAndStatus(UUID branchId, String status);
    Optional<CashSession> findByIdAndTenantId(UUID id, UUID tenantId);
    List<CashSession> findByTenantId(UUID tenantId);

    // Historial de cierres (arqueos) paginado, mas reciente primero.
    org.springframework.data.domain.Page<CashSession> findByTenantIdAndStatusOrderByClosedAtDesc(
            UUID tenantId, String status, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<CashSession> findByTenantIdAndBranchIdAndStatusOrderByClosedAtDesc(
            UUID tenantId, UUID branchId, String status, org.springframework.data.domain.Pageable pageable);
}
