package com.sumaup360.erp.restaurant.repository;

import com.sumaup360.erp.restaurant.domain.KitchenOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KitchenOrderItemRepository extends JpaRepository<KitchenOrderItem, UUID> {
    List<KitchenOrderItem> findByKitchenOrderId(UUID kitchenOrderId);
    Optional<KitchenOrderItem> findByIdAndTenantId(UUID id, UUID tenantId);
}
