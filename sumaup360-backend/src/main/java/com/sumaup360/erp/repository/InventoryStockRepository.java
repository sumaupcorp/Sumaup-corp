package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.InventoryStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryStockRepository extends JpaRepository<InventoryStock, UUID> {
    Optional<InventoryStock> findByTenantIdAndProductIdAndBranchId(UUID tenantId, UUID productId, UUID branchId);
    List<InventoryStock> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
    List<InventoryStock> findByTenantIdAndProductId(UUID tenantId, UUID productId);

    /** Stock bajo: productos en una sucursal con cantidad <= umbral. */
    List<InventoryStock> findByTenantIdAndBranchIdAndQuantityLessThanEqual(
            UUID tenantId, UUID branchId, BigDecimal threshold);
}
