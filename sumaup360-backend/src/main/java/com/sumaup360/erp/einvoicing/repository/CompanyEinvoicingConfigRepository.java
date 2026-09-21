package com.sumaup360.erp.einvoicing.repository;

import com.sumaup360.erp.einvoicing.model.CompanyEinvoicingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyEinvoicingConfigRepository extends JpaRepository<CompanyEinvoicingConfig, UUID> {

    Optional<CompanyEinvoicingConfig> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
}
