package com.sumaup360.erp.documenttemplate.model;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/** Auditoria de cambios de plantilla (CREATE / UPDATE / SET_DEFAULT). */
@Entity
@Table(name = "document_template_audit", schema = "erp")
@Getter
@Setter
public class DocumentTemplateAudit extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "action", nullable = false, length = 40)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_data", columnDefinition = "jsonb")
    private String previousData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "jsonb")
    private String newData;

    @Column(name = "user_id")
    private UUID userId;
}
