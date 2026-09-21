package com.sumaup360.erp.accounting.model;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Cuenta del plan contable (PCGE). company_id/tenant_id NULL = plantilla global
 * compartida; con valores = cuenta propia de una empresa.
 */
@Entity
@Table(name = "account", schema = "erp")
@Getter
@Setter
public class Account extends BaseEntity {

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "account_class")
    private Short accountClass;

    @Column(name = "nature", length = 10)
    private String nature;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
