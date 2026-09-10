package com.sumaup360.erp.documenttemplate.repository;

import com.sumaup360.erp.documenttemplate.model.IssuedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssuedDocumentRepository extends JpaRepository<IssuedDocument, UUID> {

    List<IssuedDocument> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);

    Optional<IssuedDocument> findByIdAndTenantIdAndCompanyId(UUID id, UUID tenantId, UUID companyId);

    /** Documento emitido para una venta (el ticket del POS). */
    Optional<IssuedDocument> findFirstBySaleIdAndTenantIdOrderByCreatedAtDesc(UUID saleId, UUID tenantId);
}
