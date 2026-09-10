package com.sumaup360.erp.documenttemplate.repository;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.model.DocumentTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTemplateRepository extends JpaRepository<DocumentTemplate, UUID> {

    List<DocumentTemplate> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);

    List<DocumentTemplate> findByTenantIdAndCompanyIdAndDocumentType(
            UUID tenantId, UUID companyId, DocumentType documentType);

    Optional<DocumentTemplate> findByIdAndTenantIdAndCompanyId(UUID id, UUID tenantId, UUID companyId);

    Optional<DocumentTemplate> findByTenantIdAndCompanyIdAndDocumentTypeAndDefaultTemplateTrue(
            UUID tenantId, UUID companyId, DocumentType documentType);

    boolean existsByTenantIdAndCompanyIdAndTemplateCode(UUID tenantId, UUID companyId, String templateCode);
}
