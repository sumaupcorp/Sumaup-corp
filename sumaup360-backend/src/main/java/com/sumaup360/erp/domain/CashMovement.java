package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Movimiento de efectivo de una caja abierta, fuera de las ventas: ingresos (sencillo,
 * cobranzas) y salidas (gastos menores, pagos a proveedor, retiros del dueno, remesas).
 * Todo movimiento afecta el efectivo esperado del arqueo al cierre.
 */
@Entity
@Table(name = "cash_movement", schema = "erp")
@Getter
@Setter
public class CashMovement extends BaseEntity {

    /** Entra efectivo a caja. */
    public static final String TYPE_INCOME = "INCOME";
    /** Sale efectivo de caja. */
    public static final String TYPE_EXPENSE = "EXPENSE";

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "cash_session_id", nullable = false)
    private UUID cashSessionId;

    @Column(name = "type", nullable = false, length = 10)
    private String type;

    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Column(name = "concept", nullable = false, length = 200)
    private String concept;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_by")
    private UUID createdBy;
}
