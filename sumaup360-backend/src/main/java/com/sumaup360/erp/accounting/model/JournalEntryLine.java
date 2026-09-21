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

/** Linea (detalle) de un asiento contable: una cuenta con importe al debe o al haber. */
@Entity
@Table(name = "journal_entry_line", schema = "erp")
@Getter
@Setter
public class JournalEntryLine extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "journal_entry_id", nullable = false)
    private UUID journalEntryId;

    @Column(name = "account_code", nullable = false, length = 20)
    private String accountCode;

    @Column(name = "glosa", length = 200)
    private String glosa;

    @Column(name = "debe", nullable = false, precision = 14, scale = 2)
    private BigDecimal debe = BigDecimal.ZERO;

    @Column(name = "haber", nullable = false, precision = 14, scale = 2)
    private BigDecimal haber = BigDecimal.ZERO;

    /** Tipo de comprobante SUNAT (tabla 10): 01 factura, 03 boleta, 07 NC, 08 ND. */
    @Column(name = "doc_tipo_sunat", length = 2)
    private String docTipoSunat;

    @Column(name = "doc_serie_numero", length = 40)
    private String docSerieNumero;

    @Column(name = "doc_fecha")
    private LocalDate docFecha;

    /** Tipo de documento de identidad del tercero (tabla 6 SUNAT): 1 DNI, 6 RUC. */
    @Column(name = "tercero_doc_tipo", length = 2)
    private String terceroDocTipo;

    @Column(name = "tercero_doc_num", length = 20)
    private String terceroDocNum;

    @Column(name = "tercero_nombre", length = 200)
    private String terceroNombre;

    @Column(name = "orden", nullable = false)
    private int orden = 0;
}
