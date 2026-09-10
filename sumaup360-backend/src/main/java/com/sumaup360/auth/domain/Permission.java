package com.sumaup360.auth.domain;

import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Permiso global con formato recurso:accion (p. ej. receipt:process, tenant:read).
 * Es la unidad atomica de autorizacion; los roles agrupan permisos.
 */
@Entity
@Table(name = "permission", schema = "auth")
@Getter
@Setter
public class Permission extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "description", length = 200)
    private String description;
}
