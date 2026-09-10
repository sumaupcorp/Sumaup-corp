package com.sumaup360.tenant.repository;

import com.sumaup360.tenant.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findByTenantId(UUID tenantId);
    Optional<Company> findByIdAndTenantId(UUID id, UUID tenantId);
}
