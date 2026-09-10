package com.sumaup360.erp.module.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Modulo habilitado (o no) para una empresa. source DEFAULT = vino del rubro;
 * OVERRIDE = lo ajusto el administrador.
 */
@Entity
@Table(name = "company_module", schema = "erp")
@Getter
@Setter
public class CompanyModule extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "module_code", nullable = false, length = 40)
    private String moduleCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "source", nullable = false, length = 20)
    private String source = "DEFAULT";
}
