package com.sumaup360.erp.batch.repository;

import com.sumaup360.erp.batch.domain.ProductBatch;
import com.sumaup360.erp.batch.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID> {

    Optional<ProductBatch> findByIdAndTenantId(UUID id, UUID tenantId);

    /** En PostgreSQL el ASC deja los NULL (sin vencimiento) al final. */
    List<ProductBatch> findByTenantIdAndBranchIdOrderByExpiryDateAsc(UUID tenantId, UUID branchId);

    List<ProductBatch> findByTenantIdAndBranchIdAndProductIdOrderByExpiryDateAsc(
            UUID tenantId, UUID branchId, UUID productId);

    Optional<ProductBatch> findByTenantIdAndBranchIdAndProductIdAndBatchCodeIgnoreCase(
            UUID tenantId, UUID branchId, UUID productId, String batchCode);

    /** Lotes activos que vencen hasta la fecha dada (alertas de vencimiento). */
    List<ProductBatch> findByTenantIdAndStatusAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
            UUID tenantId, BatchStatus status, LocalDate until);
}
