package com.sumaup360.erp.batch.domain;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.batch.enums.BatchStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Lote de un producto por sucursal, con fecha de vencimiento (farmacia, panaderia, minimarket). */
@Entity
@Table(name = "product_batch", schema = "erp")
@Getter
@Setter
public class ProductBatch extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "batch_code", nullable = false, length = 60)
    private String batchCode;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status = BatchStatus.ACTIVE;

    @Column(name = "notes", length = 300)
    private String notes;
}
