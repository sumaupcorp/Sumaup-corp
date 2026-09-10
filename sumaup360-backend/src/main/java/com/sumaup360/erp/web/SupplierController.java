package com.sumaup360.erp.web;

import com.sumaup360.erp.service.SupplierService;
import com.sumaup360.erp.web.dto.SupplierDtos.CreateSupplierRequest;
import com.sumaup360.erp.web.dto.SupplierDtos.SupplierResponse;
import com.sumaup360.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Proveedores del tenant del contexto. */
@RestController
@RequestMapping("/api/v1/suppliers")
@Tag(name = "Proveedores", description = "Proveedores del negocio")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('supplier:manage')")
    @Operation(summary = "Crea un proveedor (requiere supplier:manage)")
    public SupplierResponse create(@Valid @RequestBody CreateSupplierRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return SupplierResponse.from(supplierService.create(
                tenantId, req.companyId(), req.name(), req.ruc(), req.phone(), req.email(),
                req.contactName(), req.address(), req.notes()));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:manage')")
    @Operation(summary = "Actualiza un proveedor (requiere supplier:manage)")
    public SupplierResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody com.sumaup360.erp.web.dto.SupplierDtos.UpdateSupplierRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return SupplierResponse.from(supplierService.update(
                id, tenantId, req.name(), req.ruc(), req.phone(), req.email(),
                req.contactName(), req.address(), req.notes(), req.active()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('supplier:read')")
    @Operation(summary = "Lista proveedores de la empresa (requiere supplier:read)")
    public List<SupplierResponse> list(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return supplierService.list(tenantId, companyId).stream().map(SupplierResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('supplier:read')")
    @Operation(summary = "Obtiene un proveedor (requiere supplier:read)")
    public SupplierResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return SupplierResponse.from(supplierService.get(id, tenantId));
    }
}
