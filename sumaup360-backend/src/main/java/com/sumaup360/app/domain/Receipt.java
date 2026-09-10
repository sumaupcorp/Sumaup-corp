package com.sumaup360.app.domain;

import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.enums.ReceiptType;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Recibo/comprobante subido por la persona. Lo procesa el Backoffice (Fase 8). */
@Entity
@Table(name = "receipt", schema = "app")
@Getter
@Setter
public class Receipt extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ReceiptType type = ReceiptType.OTHER;

    @Column(name = "doc_number", length = 40)
    private String docNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PEN";

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    /** Ruta del archivo en Firebase Storage (ej. receipts/{userId}/{receiptId}.jpg). */
    @Column(name = "file_path", length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceiptStatus status = ReceiptStatus.PENDING;

    @Column(name = "notes", length = 255)
    private String notes;

    /** Staff (contador) que tomo el recibo en el Backoffice. */
    @Column(name = "assigned_staff_id")
    private UUID assignedStaffId;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    /** RUC del emisor del comprobante (de quien emitio la boleta/factura). */
    @Column(name = "issuer_ruc", length = 11)
    private String issuerRuc;

    @Column(name = "declared_sire", nullable = false)
    private boolean declaredSire = false;

    @Column(name = "sire_period", length = 7)
    private String sirePeriod;

    @Column(name = "sire_declared_at")
    private OffsetDateTime sireDeclaredAt;

    @Column(name = "declared_sunat", nullable = false)
    private boolean declaredSunat = false;

    @Column(name = "sunat_period", length = 7)
    private String sunatPeriod;

    @Column(name = "sunat_declared_at")
    private OffsetDateTime sunatDeclaredAt;
}

