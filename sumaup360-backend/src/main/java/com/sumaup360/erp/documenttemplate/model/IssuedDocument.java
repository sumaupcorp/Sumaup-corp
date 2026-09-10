package com.sumaup360.erp.documenttemplate.model;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.documenttemplate.enums.DocumentStatus;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Metadatos de un documento emitido. Preparado para facturacion electronica:
 * xml_url, cdr_url, qr_value, hash_value y sunat_status son TODO de la integracion SUNAT.
 */
@Entity
@Table(name = "issued_documents", schema = "erp")
@Getter
@Setter
public class IssuedDocument extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 20)
    private DocumentStatus documentStatus = DocumentStatus.DRAFT;

    @Column(name = "series", length = 10)
    private String series;

    @Column(name = "number")
    private Long number;

    @Column(name = "full_number", length = 40)
    private String fullNumber;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "sale_id")
    private UUID saleId;

    @Column(name = "quotation_id")
    private UUID quotationId;

    @Column(name = "delivery_guide_id")
    private UUID deliveryGuideId;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "PEN";

    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    // --- TODO SUNAT: campos de facturacion electronica (no usar todavia) ---
    @Column(name = "xml_url", length = 500)
    private String xmlUrl;

    @Column(name = "cdr_url", length = 500)
    private String cdrUrl;

    @Column(name = "qr_value", length = 500)
    private String qrValue;

    @Column(name = "hash_value", length = 200)
    private String hashValue;

    @Column(name = "sunat_status", length = 40)
    private String sunatStatus;
    // --- fin TODO SUNAT ---

    @Column(name = "issued_at")
    private OffsetDateTime issuedAt;
}
