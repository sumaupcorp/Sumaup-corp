package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findByTenantId(UUID tenantId);

    Optional<Sale> findByIdAndTenantId(UUID id, UUID tenantId);

    /** Ventas de una sesion de caja (para el resumen y arqueo). */
    List<Sale> findByCashSessionId(UUID cashSessionId);

    /** Historial de compras de un cliente (para su ficha y estadisticas). */
    List<Sale> findByTenantIdAndCustomerIdOrderByCreatedAtDesc(UUID tenantId, UUID customerId);

    /** Ventas de una sucursal desde una fecha (para agregados como el flujo semanal). */
    List<Sale> findByTenantIdAndBranchIdAndCreatedAtGreaterThanEqual(
            UUID tenantId, UUID branchId, OffsetDateTime from);

    /** Ventas del tenant desde una fecha (agregados consolidados de reportes). */
    List<Sale> findByTenantIdAndCreatedAtGreaterThanEqual(UUID tenantId, OffsetDateTime from);

    // Listado paginado con filtros indexables (sucursal, sesion de caja).
    org.springframework.data.domain.Page<Sale> findByTenantId(
            UUID tenantId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Sale> findByTenantIdAndBranchId(
            UUID tenantId, UUID branchId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Sale> findByTenantIdAndCashSessionId(
            UUID tenantId, UUID cashSessionId, org.springframework.data.domain.Pageable pageable);

    /** Resumen [count, sum(total)] de ventas del tenant en el rango (sucursal opcional). */
    @Query("""
            SELECT COUNT(s.id), COALESCE(SUM(s.total), 0)
            FROM Sale s
            WHERE s.tenantId = :tenantId
              AND s.createdAt BETWEEN :from AND :to
              AND (:branchId IS NULL OR s.branchId = :branchId)
            """)
    List<Object[]> salesSummary(@Param("tenantId") UUID tenantId,
                                @Param("from") OffsetDateTime from,
                                @Param("to") OffsetDateTime to,
                                @Param("branchId") UUID branchId);

    /** [branchId, count, sum(total)] agrupado por sucursal. */
    @Query("""
            SELECT s.branchId, COUNT(s.id), COALESCE(SUM(s.total), 0)
            FROM Sale s
            WHERE s.tenantId = :tenantId
              AND s.createdAt BETWEEN :from AND :to
            GROUP BY s.branchId
            """)
    List<Object[]> salesByBranch(@Param("tenantId") UUID tenantId,
                                 @Param("from") OffsetDateTime from,
                                 @Param("to") OffsetDateTime to);
}
