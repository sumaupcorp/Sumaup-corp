package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Venta registrada en una sucursal, dentro de una caja abierta. */
@Entity
@Table(name = "sale", schema = "erp")
@Getter
@Setter
public class Sale extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "cash_session_id")
    private UUID cashSessionId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMPLETED";

    /** Metodo de pago: CASH, CARD, YAPE, PLIN, TRANSFER. Solo CASH suma al arqueo. */
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod = "CASH";

    @Column(name = "created_by")
    private UUID createdBy;
}
