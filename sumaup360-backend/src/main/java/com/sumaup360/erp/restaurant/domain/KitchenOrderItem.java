package com.sumaup360.erp.restaurant.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.restaurant.enums.OrderItemStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Item de una comanda (plato/producto) con su estado en cocina. */
@Entity
@Table(name = "kitchen_order_item", schema = "erp")
@Getter
@Setter
public class KitchenOrderItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "kitchen_order_id", nullable = false)
    private UUID kitchenOrderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "notes", length = 200)
    private String notes;

    @Column(name = "station", length = 60)
    private String station;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderItemStatus status = OrderItemStatus.PENDING;
}
