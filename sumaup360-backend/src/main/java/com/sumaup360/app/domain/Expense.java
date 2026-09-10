package com.sumaup360.app.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Gasto registrado por la persona. */
@Entity
@Table(name = "expense", schema = "app")
@Getter
@Setter
public class Expense extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tx_date", nullable = false)
    private LocalDate txDate;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PEN";

    @Column(name = "category", length = 60)
    private String category;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "receipt_id")
    private UUID receiptId;
}
