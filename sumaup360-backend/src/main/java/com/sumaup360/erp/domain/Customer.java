package com.sumaup360.erp.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Cliente del negocio (tenant). */
@Entity
@Table(name = "customer", schema = "erp")
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Empresa a la que pertenece el cliente (aislamiento por negocio del tenant). */
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "doc_type", length = 20)
    private String docType;

    @Column(name = "doc_number", length = 20)
    private String docNumber;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;
}
