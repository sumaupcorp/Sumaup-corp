package com.sumaup360.erp.restaurant.repository;

import com.sumaup360.erp.restaurant.domain.KitchenOrder;
import com.sumaup360.erp.restaurant.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KitchenOrderRepository extends JpaRepository<KitchenOrder, UUID> {
    List<KitchenOrder> findByTenantIdAndBranchId(UUID tenantId, UUID branchId);
    List<KitchenOrder> findByTenantIdAndBranchIdAndStatus(UUID tenantId, UUID branchId, OrderStatus status);
    Optional<KitchenOrder> findByIdAndTenantId(UUID id, UUID tenantId);
    long countByBranchId(UUID branchId);
}
