package com.sumaup360.erp.customorder.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.customorder.enums.CustomOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Pedido por encargo (modulo custom-orders: panaderia, pasteleria). */
@Entity
@Table(name = "custom_order", schema = "erp")
@Getter
@Setter
public class CustomOrder extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "delivery_at", nullable = false)
    private OffsetDateTime deliveryAt;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "advance_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal advanceAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CustomOrderStatus status = CustomOrderStatus.PENDING;

    /** Venta generada al cobrar el encargo (TODO: integrar cobro via POS). */
    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "notes", length = 300)
    private String notes;
}
