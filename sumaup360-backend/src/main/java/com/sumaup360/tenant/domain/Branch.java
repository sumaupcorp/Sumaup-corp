package com.sumaup360.tenant.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Sucursal de una empresa. Lleva tenant_id para el aislamiento multi-tenant. */
@Entity
@Table(name = "branch", schema = "tenant")
@Getter
@Setter
public class Branch extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "is_main", nullable = false)
    private boolean main;
}
