package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/** Movimiento de inventario (auditoria). type: IN, OUT, ADJUST. */
@Entity
@Table(name = "inventory_movement", schema = "erp")
@Getter
@Setter
public class InventoryMovement extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "reason", length = 160)
    private String reason;

    @Column(name = "ref_id")
    private UUID refId;

    @Column(name = "created_by")
    private UUID createdBy;
}
