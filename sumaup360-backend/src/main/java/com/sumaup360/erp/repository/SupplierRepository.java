package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    List<Supplier> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
    Optional<Supplier> findByIdAndTenantId(UUID id, UUID tenantId);
}
