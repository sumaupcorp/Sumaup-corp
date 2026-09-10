package com.sumaup360.tenant.web;

import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.TenantUserService;
import com.sumaup360.tenant.web.dto.TenantUserDtos.AssignRoleRequest;
import com.sumaup360.tenant.web.dto.TenantUserDtos.CreateWorkerRequest;
import com.sumaup360.tenant.web.dto.TenantUserDtos.TenantUserResponse;
import com.sumaup360.tenant.web.dto.TenantUserDtos.UpdateAssignmentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Gestion de usuarios (trabajadores) del tenant del contexto: listarlos y asignar/quitar
 * sus roles del tenant. Consultar requiere user:read; modificar requiere user:manage.
 */
@RestController
@RequestMapping("/api/v1/tenant-users")
@Tag(name = "Usuarios del tenant", description = "Trabajadores de la empresa y sus roles")
public class TenantUserController {

    private final TenantUserService tenantUserService;

    public TenantUserController(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    @Operation(summary = "Lista los usuarios del tenant con roles, sede y modulos (requiere user:read)")
    public List<TenantUserResponse> list() {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return tenantUserService.listTenantUsers(tenantId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('user:manage')")
    @Operation(summary = "Crea un trabajador: cuenta + rol/sede/modulos (requiere user:manage)")
    public TenantUserResponse createWorker(@Valid @RequestBody CreateWorkerRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return tenantUserService.createWorker(tenantId, req);
    }

    @PutMapping("/{userId}/assignment")
    @PreAuthorize("hasAuthority('user:manage')")
    @Operation(summary = "Cambia la sede asignada y los modulos visibles (requiere user:manage)")
    public TenantUserResponse updateAssignment(@PathVariable UUID userId,
                                               @Valid @RequestBody UpdateAssignmentRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return tenantUserService.updateAssignment(tenantId, userId, req);
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('user:manage')")
    @Operation(summary = "Asigna un rol del tenant a un usuario (requiere user:manage)")
    public TenantUserResponse assignRole(@PathVariable UUID userId,
                                         @Valid @RequestBody AssignRoleRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return tenantUserService.assignRole(tenantId, userId, req.roleId());
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('user:manage')")
    @Operation(summary = "Quita un rol del tenant a un usuario (requiere user:manage)")
    public TenantUserResponse revokeRole(@PathVariable UUID userId, @PathVariable UUID roleId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return tenantUserService.revokeRole(tenantId, userId, roleId);
    }
}
