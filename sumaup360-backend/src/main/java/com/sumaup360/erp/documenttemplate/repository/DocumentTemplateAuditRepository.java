package com.sumaup360.erp.documenttemplate.repository;

import com.sumaup360.erp.documenttemplate.model.DocumentTemplateAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentTemplateAuditRepository extends JpaRepository<DocumentTemplateAudit, UUID> {
    List<DocumentTemplateAudit> findByTemplateId(UUID templateId);
}
