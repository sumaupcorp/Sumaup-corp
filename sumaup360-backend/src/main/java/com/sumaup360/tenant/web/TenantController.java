package com.sumaup360.tenant.web;

import com.sumaup360.tenant.service.TenantService;
import com.sumaup360.tenant.web.dto.TenantDtos.CreateTenantRequest;
import com.sumaup360.tenant.web.dto.TenantDtos.TenantResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Gestion de tenants. Operacion cross-tenant tipica del Backoffice/staff:
 * crear (tenant:manage) y consultar (tenant:read).
 */
@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Cuentas cliente (Linea Negocios)")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tenant:manage')")
    @Operation(summary = "Crea un tenant (requiere tenant:manage)")
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest req) {
        return TenantResponse.from(tenantService.create(req.code(), req.name()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tenant:read')")
    @Operation(summary = "Lista los tenants (requiere tenant:read)")
    public List<TenantResponse> list() {
        return tenantService.list().stream().map(TenantResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tenant:read')")
    @Operation(summary = "Obtiene un tenant (requiere tenant:read)")
    public TenantResponse get(@PathVariable UUID id) {
        return TenantResponse.from(tenantService.get(id));
    }
}
