package com.sumaup360.erp.accounting.repository;

import com.sumaup360.erp.accounting.model.AccountingConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountingConfigRepository extends JpaRepository<AccountingConfig, UUID> {

    Optional<AccountingConfig> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
}
