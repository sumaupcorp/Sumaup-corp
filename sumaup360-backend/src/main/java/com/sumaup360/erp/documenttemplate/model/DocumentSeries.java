package com.sumaup360.erp.documenttemplate.model;

import com.sumaup360.common.BaseEntity;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Serie y correlativo por tipo de documento. Preparado para SUNAT (correlativos oficiales). */
@Entity
@Table(name = "document_series", schema = "erp")
@Getter
@Setter
public class DocumentSeries extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "branch_id")
    private UUID branchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 40)
    private DocumentType documentType;

    @Column(name = "series", nullable = false, length = 10)
    private String series;

    @Column(name = "current_number", nullable = false)
    private long currentNumber = 0;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
