package com.sumaup360.erp.accounting.model;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Asiento contable (cabecera). El detalle vive en JournalEntryLine. */
@Entity
@Table(name = "journal_entry", schema = "erp")
@Getter
@Setter
public class JournalEntry extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "subdiario", nullable = false, length = 4)
    private String subdiario;

    @Column(name = "correlativo", length = 20)
    private String correlativo;

    @Column(name = "glosa", length = 200)
    private String glosa;

    @Column(name = "moneda", nullable = false, length = 2)
    private String moneda = "MN";

    @Column(name = "tipo_cambio", precision = 10, scale = 3)
    private BigDecimal tipoCambio;

    /** MANUAL | SALE. */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "total_debe", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDebe = BigDecimal.ZERO;

    @Column(name = "total_haber", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalHaber = BigDecimal.ZERO;
}
