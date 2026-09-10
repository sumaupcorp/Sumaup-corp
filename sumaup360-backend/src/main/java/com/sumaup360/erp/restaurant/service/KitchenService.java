package com.sumaup360.erp.restaurant.service;

import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.restaurant.domain.KitchenOrder;
import com.sumaup360.erp.restaurant.domain.KitchenOrderItem;
import com.sumaup360.erp.restaurant.enums.OrderItemStatus;
import com.sumaup360.erp.restaurant.enums.OrderStatus;
import com.sumaup360.erp.restaurant.repository.KitchenOrderItemRepository;
import com.sumaup360.erp.restaurant.repository.KitchenOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Cocina: cola de comandas en preparacion y cambio de estado de cada item. */
@Service
public class KitchenService {

    private final KitchenOrderRepository orderRepository;
    private final KitchenOrderItemRepository itemRepository;

    public KitchenService(KitchenOrderRepository orderRepository,
                          KitchenOrderItemRepository itemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
    }

    /** Comandas que estan en cocina (enviadas y aun no servidas/cobradas). */
    @Transactional(readOnly = true)
    public List<KitchenOrder> queue(UUID tenantId, UUID branchId) {
        return orderRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, OrderStatus.IN_KITCHEN);
    }

    @Transactional(readOnly = true)
    public List<KitchenOrderItem> itemsOf(UUID orderId) {
        return itemRepository.findByKitchenOrderId(orderId);
    }

    /** Cambia el estado de un item y recalcula el estado de su comanda. */
    @Transactional
    public KitchenOrderItem setItemStatus(UUID tenantId, UUID itemId, OrderItemStatus status) {
        KitchenOrderItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Item de comanda no encontrado."));
        item.setStatus(status);
        itemRepository.save(item);
        recalcOrder(tenantId, item.getKitchenOrderId());
        return item;
    }

    private void recalcOrder(UUID tenantId, UUID orderId) {
        KitchenOrder order = orderRepository.findByIdAndTenantId(orderId, tenantId).orElse(null);
        if (order == null) {
            return;
        }
        if (order.getStatus() == OrderStatus.OPEN
                || order.getStatus() == OrderStatus.BILLED
                || order.getStatus() == OrderStatus.CANCELLED) {
            return;
        }
        List<KitchenOrderItem> items = itemRepository.findByKitchenOrderId(orderId);
        boolean allServed = items.stream().allMatch(i -> i.getStatus() == OrderItemStatus.SERVED);
        boolean allReady = items.stream()
                .allMatch(i -> i.getStatus() == OrderItemStatus.READY || i.getStatus() == OrderItemStatus.SERVED);
        if (allServed) {
            order.setStatus(OrderStatus.SERVED);
        } else if (allReady) {
            order.setStatus(OrderStatus.READY);
        } else {
            order.setStatus(OrderStatus.IN_KITCHEN);
        }
        orderRepository.save(order);
    }
}
