package com.sumaup360.erp.repository;

import com.sumaup360.erp.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByTenantIdAndCompanyId(UUID tenantId, UUID companyId);
    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndCompanyIdAndSku(UUID tenantId, UUID companyId, String sku);
}
