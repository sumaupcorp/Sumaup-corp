package com.sumaup360.auth.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Rol = conjunto de permisos.
 * tenant_id NULL  -> rol global (staff / Backoffice).
 * tenant_id valor -> rol propio de un tenant (cajero, almacen, etc.).
 */
@Entity
@Table(name = "role", schema = "auth")
@Getter
@Setter
public class Role extends BaseEntity {

    @Column(name = "code", nullable = false, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** NULL para roles globales/staff. */
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "is_staff", nullable = false)
    private boolean staff;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission", schema = "auth",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();
}
