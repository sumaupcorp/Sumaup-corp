package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Sesion de caja (POS) de una sucursal. Solo una abierta por sucursal a la vez. */
@Entity
@Table(name = "cash_session", schema = "erp")
@Getter
@Setter
public class CashSession extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "opened_by")
    private UUID openedBy;

    /** Fondo inicial de caja (sencillo para vuelto). */
    @Column(name = "opening_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal openingAmount = BigDecimal.ZERO;

    /** Efectivo contado fisicamente al cierre (arqueo). */
    @Column(name = "closing_amount", precision = 12, scale = 2)
    private BigDecimal closingAmount;

    /** Efectivo esperado al cierre: fondo + ventas en efectivo + ingresos - salidas. */
    @Column(name = "expected_amount", precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    /** Contado - esperado: positivo = sobrante, negativo = faltante. */
    @Column(name = "difference", precision = 12, scale = 2)
    private BigDecimal difference;

    @Column(name = "notes", length = 300)
    private String notes;

    @Column(name = "closed_by")
    private UUID closedBy;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;
}
