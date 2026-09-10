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
 * Linea de una venta: producto (o descripcion libre), cantidad, precio unitario
 * y total de linea. product_id es opcional desde V54: las lineas de precio libre
 * (p.ej. noches de hospedaje) llevan su propia descripcion.
 */
@Entity
@Table(name = "sale_item", schema = "erp")
@Getter
@Setter
public class SaleItem extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sale_id", nullable = false)
    private UUID saleId;

    @Column(name = "product_id")
    private UUID productId;

    /** Descripcion de la linea cuando no hay producto (se pinta tal cual en el ticket). */
    @Column(name = "description", length = 160)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;
}
