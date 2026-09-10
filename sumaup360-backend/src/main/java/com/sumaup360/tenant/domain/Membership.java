package com.sumaup360.tenant.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Vinculo usuario <-> tenant. is_default indica el tenant activo por defecto del usuario.
 * Es lo que permite al backend resolver el tenant del contexto en cada request.
 */
@Entity
@Table(name = "membership", schema = "tenant")
@Getter
@Setter
public class Membership extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "is_default", nullable = false)
    private boolean defaultTenant = true;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    /** Sede asignada: null = puede operar en todas; con valor, solo en esa sucursal. */
    @Column(name = "branch_id")
    private UUID branchId;

    /** JSON array de modulos visibles para este usuario (null = todos los habilitados). */
    @Column(name = "allowed_modules")
    private String allowedModules;
}
