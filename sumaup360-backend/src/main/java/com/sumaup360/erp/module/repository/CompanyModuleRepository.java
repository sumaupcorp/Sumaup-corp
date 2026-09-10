package com.sumaup360.erp.module.repository;

import com.sumaup360.erp.module.domain.CompanyModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyModuleRepository extends JpaRepository<CompanyModule, UUID> {
    List<CompanyModule> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
    Optional<CompanyModule> findByTenantIdAndCompanyIdAndModuleCode(UUID tenantId, UUID companyId, String moduleCode);
}
