package com.sumaup360.tenant.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Empresa dentro de un tenant. business_type_code / vertical_code referencian los catalogos
 * (catalog.business_types / catalog.verticals) por codigo: el rubro define modulos, no codigo.
 */
@Entity
@Table(name = "company", schema = "tenant")
@Getter
@Setter
public class Company extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "business_type_code", length = 40)
    private String businessTypeCode;

    @Column(name = "vertical_code", length = 40)
    private String verticalCode;
}
