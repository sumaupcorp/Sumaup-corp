package com.sumaup360.tenant.web;

import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.RoleService;
import com.sumaup360.tenant.web.dto.RoleDtos.CreateRoleRequest;
import com.sumaup360.tenant.web.dto.RoleDtos.RoleResponse;
import com.sumaup360.tenant.web.dto.RoleDtos.SetPermissionsRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Roles propios del tenant del usuario (cajero, almacen, etc.). El tenant sale del contexto.
 * Crear/editar requiere role:manage; consultar requiere role:read.
 */
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles del tenant", description = "Roles y permisos propios de la empresa")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('role:manage')")
    @Operation(summary = "Crea un rol en el tenant del contexto (requiere role:manage)")
    public RoleResponse create(@Valid @RequestBody CreateRoleRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return RoleResponse.from(
                roleService.createRole(tenantId, req.code(), req.name(), req.permissionCodes()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    @Operation(summary = "Lista los roles del tenant (requiere role:read)")
    public List<RoleResponse> list() {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roleService.listByTenant(tenantId).stream().map(RoleResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    @Operation(summary = "Obtiene un rol del tenant (requiere role:read)")
    public RoleResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return RoleResponse.from(roleService.getInTenant(id, tenantId));
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:manage')")
    @Operation(summary = "Define los permisos de un rol del tenant (requiere role:manage)")
    public RoleResponse setPermissions(@PathVariable UUID id,
                                       @Valid @RequestBody SetPermissionsRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return RoleResponse.from(roleService.setPermissions(id, tenantId, req.permissionCodes()));
    }
}
