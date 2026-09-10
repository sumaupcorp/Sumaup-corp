package com.sumaup360.erp.restaurant.repository;

import com.sumaup360.erp.restaurant.domain.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {
    List<RestaurantTable> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
    Optional<RestaurantTable> findByIdAndTenantId(UUID id, UUID tenantId);
}
