package com.sumaup360.erp.documenttemplate.repository;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.model.DocumentSeries;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentSeriesRepository extends JpaRepository<DocumentSeries, UUID> {

    List<DocumentSeries> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);

    /** Series activas del tipo, con bloqueo por fila para consumir el correlativo sin duplicados. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM DocumentSeries s
            WHERE s.tenantId = :tenantId AND s.companyId = :companyId
              AND s.documentType = :documentType AND s.active = true
            ORDER BY s.series ASC
            """)
    List<DocumentSeries> findActiveForUpdate(@Param("tenantId") UUID tenantId,
                                             @Param("companyId") UUID companyId,
                                             @Param("documentType") DocumentType documentType);

    Optional<DocumentSeries> findByIdAndTenantIdAndCompanyId(UUID id, UUID tenantId, UUID companyId);

    boolean existsByTenantIdAndCompanyIdAndDocumentTypeAndSeries(
            UUID tenantId, UUID companyId, DocumentType documentType, String series);
}
