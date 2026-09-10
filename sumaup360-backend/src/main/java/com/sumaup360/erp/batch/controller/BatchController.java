package com.sumaup360.erp.batch.controller;

import com.sumaup360.erp.batch.dto.BatchDtos.BatchResponse;
import com.sumaup360.erp.batch.dto.BatchDtos.CreateBatchRequest;
import com.sumaup360.erp.batch.dto.BatchDtos.UpdateBatchRequest;
import com.sumaup360.erp.batch.service.ProductBatchService;
import com.sumaup360.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Lotes y vencimientos (modulo batch-expiry). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp/batches")
@Tag(name = "Lotes y vencimientos", description = "Lotes por producto y sucursal con alertas de vencimiento")
public class BatchController {

    private final ProductBatchService batchService;

    public BatchController(ProductBatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('batch:manage')")
    @Operation(summary = "Registra un lote (requiere batch:manage)")
    public BatchResponse create(@Valid @RequestBody CreateBatchRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return batchService.create(tenantId, req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('batch:read')")
    @Operation(summary = "Lista lotes de una sucursal, opcionalmente por producto (requiere batch:read)")
    public List<BatchResponse> list(@RequestParam UUID branchId,
                                    @RequestParam(required = false) UUID productId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return batchService.list(tenantId, branchId, productId);
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('batch:read')")
    @Operation(summary = "Lotes que vencen en los proximos dias (requiere batch:read)")
    public List<BatchResponse> expiring(@RequestParam(defaultValue = "30") int days) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return batchService.expiring(tenantId, days);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('batch:manage')")
    @Operation(summary = "Actualiza un lote: cantidad, vencimiento, estado (requiere batch:manage)")
    public BatchResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBatchRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return batchService.update(tenantId, id, req);
    }
}
