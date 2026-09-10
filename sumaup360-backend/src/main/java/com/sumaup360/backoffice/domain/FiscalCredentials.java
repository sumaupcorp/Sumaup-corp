package com.sumaup360.backoffice.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/** Credenciales fiscales de un tenant. La Clave SOL se guarda cifrada (sol_pass_enc). */
@Entity
@Table(name = "fiscal_credentials", schema = "tenant")
@Getter
@Setter
public class FiscalCredentials extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "ruc", length = 11)
    private String ruc;

    @Column(name = "sol_user", length = 64)
    private String solUser;

    @Column(name = "sol_pass_enc", columnDefinition = "text")
    private String solPassEnc;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
