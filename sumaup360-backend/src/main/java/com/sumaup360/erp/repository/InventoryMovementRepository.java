package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    List<InventoryMovement> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    /** Kardex paginado de una sucursal. */
    org.springframework.data.domain.Page<InventoryMovement> findByTenantIdAndBranchId(
            UUID tenantId, UUID branchId, org.springframework.data.domain.Pageable pageable);

    /** Movimientos desde una fecha (para agregados como el flujo semanal). */
    List<InventoryMovement> findByTenantIdAndBranchIdAndCreatedAtGreaterThanEqual(
            UUID tenantId, UUID branchId, java.time.OffsetDateTime from);
}
