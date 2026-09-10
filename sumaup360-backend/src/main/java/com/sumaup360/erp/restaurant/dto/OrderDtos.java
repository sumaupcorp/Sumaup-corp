package com.sumaup360.erp.restaurant.dto;

import com.sumaup360.erp.restaurant.domain.KitchenOrder;
import com.sumaup360.erp.restaurant.domain.KitchenOrderItem;
import com.sumaup360.erp.restaurant.enums.OrderItemStatus;
import com.sumaup360.erp.restaurant.enums.OrderStatus;
import com.sumaup360.erp.restaurant.enums.OrderType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record CreateOrderRequest(
            @NotNull UUID branchId,
            @NotNull OrderType type,
            UUID tableId,
            String notes
    ) {
    }

    public record AddItemRequest(
            @NotNull UUID productId,
            @NotNull BigDecimal quantity,
            String notes,
            String station
    ) {
    }

    public record BillOrderRequest(UUID customerId) {
    }

    public record SetItemStatusRequest(@NotNull OrderItemStatus status) {
    }

    public record OrderItemResponse(UUID id, UUID productId, BigDecimal quantity,
                                    BigDecimal unitPrice, BigDecimal lineTotal,
                                    String notes, String station, OrderItemStatus status) {
        public static OrderItemResponse from(KitchenOrderItem i) {
            return new OrderItemResponse(i.getId(), i.getProductId(), i.getQuantity(),
                    i.getUnitPrice(), i.getLineTotal(), i.getNotes(), i.getStation(), i.getStatus());
        }
    }

    public record OrderResponse(UUID id, UUID branchId, UUID tableId, String orderNumber,
                                OrderType type, OrderStatus status, String notes,
                                BigDecimal total, UUID saleId, List<OrderItemResponse> items) {
        public static OrderResponse from(KitchenOrder o, List<KitchenOrderItem> items) {
            return new OrderResponse(o.getId(), o.getBranchId(), o.getTableId(), o.getOrderNumber(),
                    o.getType(), o.getStatus(), o.getNotes(), o.getTotal(), o.getSaleId(),
                    items.stream().map(OrderItemResponse::from).toList());
        }
    }
}
