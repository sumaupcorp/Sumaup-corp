package com.sumaup360.erp.customorder.controller;

import com.sumaup360.erp.customorder.dto.CustomOrderDtos.CreateCustomOrderRequest;
import com.sumaup360.erp.customorder.dto.CustomOrderDtos.CustomOrderResponse;
import com.sumaup360.erp.customorder.dto.CustomOrderDtos.UpdateCustomOrderRequest;
import com.sumaup360.erp.customorder.dto.CustomOrderDtos.UpdateCustomOrderStatusRequest;
import com.sumaup360.erp.customorder.enums.CustomOrderStatus;
import com.sumaup360.erp.customorder.service.CustomOrderService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

/** Pedidos por encargo (modulo custom-orders). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp/custom-orders")
@Tag(name = "Pedidos por encargo", description = "Encargos con fecha de entrega y adelanto")
public class CustomOrderController {

    private final CustomOrderService customOrderService;

    public CustomOrderController(CustomOrderService customOrderService) {
        this.customOrderService = customOrderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('custom-order:manage')")
    @Operation(summary = "Registra un pedido por encargo (requiere custom-order:manage)")
    public CustomOrderResponse create(@Valid @RequestBody CreateCustomOrderRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return customOrderService.create(tenantId, req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('custom-order:read')")
    @Operation(summary = "Lista pedidos de una sucursal, opcionalmente por estado (requiere custom-order:read)")
    public List<CustomOrderResponse> list(@RequestParam UUID branchId,
                                          @RequestParam(required = false) CustomOrderStatus status) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return customOrderService.list(tenantId, branchId, status);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('custom-order:manage')")
    @Operation(summary = "Edita un pedido por encargo (requiere custom-order:manage)")
    public CustomOrderResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomOrderRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return customOrderService.update(tenantId, id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('custom-order:manage')")
    @Operation(summary = "Cambia el estado de un pedido (requiere custom-order:manage)")
    public CustomOrderResponse updateStatus(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateCustomOrderStatusRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return customOrderService.updateStatus(tenantId, id, req.status());
    }
}
