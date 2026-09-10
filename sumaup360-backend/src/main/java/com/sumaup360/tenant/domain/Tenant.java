package com.sumaup360.tenant.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Tenant: la cuenta cliente de la Linea Negocios. Agrupa empresas y es la unidad de
 * aislamiento multi-tenant (todo recurso de Negocios se filtra por su tenant_id).
 */
@Entity
@Table(name = "tenant", schema = "tenant")
@Getter
@Setter
public class Tenant extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";
}
