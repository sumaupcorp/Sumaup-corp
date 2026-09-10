package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Proveedor del negocio (tenant). */
@Entity
@Table(name = "supplier", schema = "erp")
@Getter
@Setter
public class Supplier extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Empresa a la que pertenece el proveedor (aislamiento por negocio del tenant). */
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    /** Persona de contacto (el "casero" con quien se coordina). */
    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
