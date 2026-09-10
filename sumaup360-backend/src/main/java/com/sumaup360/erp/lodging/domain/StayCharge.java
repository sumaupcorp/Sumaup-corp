package com.sumaup360.erp.lodging.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cargo a la habitacion durante la estadia (minibar, lavanderia, desayuno...).
 * product_id opcional: si viene del inventario, descuenta stock al liquidarse
 * en el check-out como linea de la venta POS.
 */
@Entity
@Table(name = "stay_charge", schema = "erp")
@Getter
@Setter
public class StayCharge extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "stay_id", nullable = false)
    private UUID stayId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "description", nullable = false, length = 160)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "created_by")
    private UUID createdBy;
}
