package com.sumaup360.erp.restaurant.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.restaurant.enums.OrderStatus;
import com.sumaup360.erp.restaurant.enums.OrderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Comanda: pedido de una mesa (o para llevar/delivery) que pasa por cocina y luego se cobra. */
@Entity
@Table(name = "kitchen_order", schema = "erp")
@Getter
@Setter
public class KitchenOrder extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "table_id")
    private UUID tableId;

    @Column(name = "order_number", nullable = false, length = 30)
    private String orderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private OrderType type = OrderType.DINE_IN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status = OrderStatus.OPEN;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "created_by")
    private UUID createdBy;
}
