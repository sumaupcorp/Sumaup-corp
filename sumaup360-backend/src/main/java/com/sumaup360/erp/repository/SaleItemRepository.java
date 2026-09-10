package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.SaleItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    List<SaleItem> findBySaleId(UUID saleId);

    /** [productId, sum(quantity), sum(lineTotal)] mas vendidos en el rango (branchId opcional). */
    @Query("""
            SELECT si.productId, SUM(si.quantity), SUM(si.lineTotal)
            FROM SaleItem si, Sale s
            WHERE si.saleId = s.id
              AND si.productId IS NOT NULL
              AND s.tenantId = :tenantId
              AND s.createdAt BETWEEN :from AND :to
              AND (:branchId IS NULL OR s.branchId = :branchId)
            GROUP BY si.productId
            ORDER BY SUM(si.lineTotal) DESC
            """)
    List<Object[]> topProducts(@Param("tenantId") UUID tenantId,
                               @Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to,
                               @Param("branchId") UUID branchId,
                               Pageable pageable);
}
