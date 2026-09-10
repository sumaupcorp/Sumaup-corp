package com.sumaup360.erp.customorder.repository;

import com.sumaup360.erp.customorder.domain.CustomOrder;
import com.sumaup360.erp.customorder.enums.CustomOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomOrderRepository extends JpaRepository<CustomOrder, UUID> {

    Optional<CustomOrder> findByIdAndTenantId(UUID id, UUID tenantId);

    List<CustomOrder> findByTenantIdAndBranchIdOrderByDeliveryAtAsc(UUID tenantId, UUID branchId);

    List<CustomOrder> findByTenantIdAndBranchIdAndStatusOrderByDeliveryAtAsc(
            UUID tenantId, UUID branchId, CustomOrderStatus status);
}
