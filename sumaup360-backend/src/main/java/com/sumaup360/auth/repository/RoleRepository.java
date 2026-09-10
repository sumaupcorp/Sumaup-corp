package com.sumaup360.auth.repository;

import com.sumaup360.auth.domain.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    /** Rol global/staff (tenant_id NULL) por codigo. */
    Optional<Role> findByCodeAndTenantIdIsNull(String code);

    /** Rol propio de un tenant por codigo. */
    Optional<Role> findByCodeAndTenantId(String code, UUID tenantId);

    /** Roles de un tenant, con sus permisos cargados (para listar/mapear sin OSIV). */
    @EntityGraph(attributePaths = "permissions")
    List<Role> findByTenantId(UUID tenantId);

    /** Rol de un tenant por id, con permisos cargados. Garantiza el aislamiento por tenant. */
    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByIdAndTenantId(UUID id, UUID tenantId);
}
