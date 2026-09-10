package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Customer> findFirstByTenantIdAndCompanyIdAndPhone(UUID tenantId, UUID companyId, String phone);
}
