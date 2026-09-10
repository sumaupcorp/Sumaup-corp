package com.sumaup360.tenant.repository;

import com.sumaup360.tenant.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {
    List<Branch> findByTenantId(UUID tenantId);
    List<Branch> findByCompanyId(UUID companyId);
    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);
}
