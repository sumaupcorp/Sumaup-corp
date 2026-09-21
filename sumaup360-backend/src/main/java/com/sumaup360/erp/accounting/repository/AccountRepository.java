package com.sumaup360.erp.accounting.repository;

import com.sumaup360.erp.accounting.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    /** Plantilla global PCGE (sin tenant/empresa). */
    List<Account> findByTenantIdIsNullAndCompanyIdIsNull();

    /** Cuentas propias de una empresa. */
    List<Account> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
}
