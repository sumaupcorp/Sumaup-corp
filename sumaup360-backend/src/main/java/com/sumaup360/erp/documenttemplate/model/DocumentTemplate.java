package com.sumaup360.erp.documenttemplate.model;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Plantilla configurable de un documento. UNA base por formato; los flags y *_config (JSON)
 * controlan que se muestra. No se duplica por rubro: business_type_id es opcional.
 */
@Entity
@Table(name = "document_templates", schema = "erp")
@Getter
@Setter
public class DocumentTemplate extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Column(name = "business_type_id")
    private UUID businessTypeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "print_format", nullable = false, length = 20)
    private PrintFormat printFormat;

    @Column(name = "template_name", nullable = false, length = 160)
    private String templateName;

    @Column(name = "template_code", nullable = false, length = 60)
    private String templateCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultTemplate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "primary_color", nullable = false, length = 20)
    private String primaryColor = "#0B5BFF";

    @Column(name = "secondary_color", nullable = false, length = 20)
    private String secondaryColor = "#102A4C";

    @Column(name = "font_family", nullable = false, length = 80)
    private String fontFamily = "Helvetica, Arial, sans-serif";

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "show_logo", nullable = false)
    private boolean showLogo = true;

    @Column(name = "show_qr", nullable = false)
    private boolean showQr = false;

    @Column(name = "show_payment_info", nullable = false)
    private boolean showPaymentInfo = true;

    @Column(name = "show_seller", nullable = false)
    private boolean showSeller = true;

    @Column(name = "show_customer_address", nullable = false)
    private boolean showCustomerAddress = true;

    @Column(name = "show_business_extra_fields", nullable = false)
    private boolean showBusinessExtraFields = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "header_config", columnDefinition = "jsonb")
    private String headerConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "body_config", columnDefinition = "jsonb")
    private String bodyConfig;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "footer_config", columnDefinition = "jsonb")
    private String footerConfig;

    @Column(name = "custom_css", columnDefinition = "text")
    private String customCss;
}
