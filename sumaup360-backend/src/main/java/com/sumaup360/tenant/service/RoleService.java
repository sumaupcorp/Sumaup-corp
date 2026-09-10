package com.sumaup360.tenant.service;

import com.sumaup360.auth.domain.Permission;
import com.sumaup360.auth.domain.Role;
import com.sumaup360.auth.repository.PermissionRepository;
import com.sumaup360.auth.repository.RoleRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Roles propios de un tenant (cajero, almacen, etc.). Completan la jerarquia RBAC:
 * Backoffice (roles globales) -> Empresa (estos roles) -> Trabajadores.
 */
@Service
public class RoleService {

    /** Permisos del rol administrador del negocio (no incluye gestion cross-tenant). */
    private static final List<String> TENANT_ADMIN_PERMS = List.of(
            "company:read", "company:manage",
            "branch:read", "branch:manage",
            "user:read", "user:manage",
            "role:read", "role:manage",
            "report:read",
            // ERP core
            "product:read", "product:manage",
            "customer:read", "customer:manage",
            "inventory:read", "inventory:adjust",
            "pos:read", "pos:operate",
            "sale:read", "sale:create",
            "supplier:read", "supplier:manage",
            // Plantillas de documentos comerciales
            "document-template:read", "document-template:manage",
            "document-series:read", "document-series:manage",
            "document:render",
            // Vertical restaurantes
            "table:read", "table:manage",
            "order:read", "order:create", "order:manage",
            "kitchen:read", "kitchen:operate",
            // Modulos por rubro: lotes, pacientes, citas, encargos, recetas
            "batch:read", "batch:manage",
            "patient:read", "patient:manage",
            "appointment:read", "appointment:manage",
            "custom-order:read", "custom-order:manage",
            "prescription:read", "prescription:manage",
            // Modulo hospedaje (hoteles, hostales)
            "lodging:read", "lodging:manage"
    );

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /** Crea (idempotente) el rol 'tenant-admin' del tenant. Se llama al crear el tenant. */
    @Transactional
    public Role createTenantAdminRole(UUID tenantId) {
        return roleRepository.findByCodeAndTenantId("tenant-admin", tenantId)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode("tenant-admin");
                    role.setName("Administrador del negocio");
                    role.setTenantId(tenantId);
                    role.setStaff(false);
                    role.setPermissions(resolvePermissions(TENANT_ADMIN_PERMS));
                    return roleRepository.save(role);
                });
    }

    @Transactional
    public Role createRole(UUID tenantId, String code, String name, List<String> permissionCodes) {
        roleRepository.findByCodeAndTenantId(code, tenantId).ifPresent(r -> {
            throw new ConflictException("Ya existe un rol '" + code + "' en este tenant.");
        });
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setTenantId(tenantId);
        role.setStaff(false);
        role.setPermissions(resolvePermissions(permissionCodes));
        return roleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<Role> listByTenant(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Role getInTenant(UUID roleId, UUID tenantId) {
        return roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en este tenant."));
    }

    @Transactional
    public Role setPermissions(UUID roleId, UUID tenantId, List<String> permissionCodes) {
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en este tenant."));
        role.setPermissions(resolvePermissions(permissionCodes));
        return roleRepository.save(role);
    }

    /** Resuelve los permisos por codigo; falla si alguno no existe. */
    private Set<Permission> resolvePermissions(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return new HashSet<>();
        }
        List<Permission> found = permissionRepository.findByCodeIn(codes);
        if (found.size() != codes.stream().distinct().count()) {
            Set<String> foundCodes = found.stream().map(Permission::getCode).collect(Collectors.toSet());
            List<String> unknown = codes.stream().distinct()
                    .filter(c -> !foundCodes.contains(c)).toList();
            throw new BadRequestException("Permisos desconocidos: " + unknown);
        }
        return new HashSet<>(found);
    }
}
