package com.sumaup360.erp.restaurant.controller;

import com.sumaup360.erp.restaurant.dto.OrderDtos.OrderItemResponse;
import com.sumaup360.erp.restaurant.dto.OrderDtos.OrderResponse;
import com.sumaup360.erp.restaurant.dto.OrderDtos.SetItemStatusRequest;
import com.sumaup360.erp.restaurant.service.KitchenService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Vista de cocina: cola de comandas en preparacion y cambio de estado de items. */
@RestController
@RequestMapping("/api/v1/erp/restaurant/kitchen")
@Tag(name = "Restaurante - Cocina", description = "Cola de cocina y estados de preparacion")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('kitchen:read')")
    @Operation(summary = "Cola de cocina: comandas en preparacion (requiere kitchen:read)")
    public List<OrderResponse> queue(@RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return kitchenService.queue(tenantId, branchId).stream()
                .map(o -> OrderResponse.from(o, kitchenService.itemsOf(o.getId())))
                .toList();
    }

    @PatchMapping("/items/{itemId}/status")
    @PreAuthorize("hasAuthority('kitchen:operate')")
    @Operation(summary = "Cambia el estado de un item en cocina (requiere kitchen:operate)")
    public OrderItemResponse setItemStatus(@PathVariable UUID itemId,
                                           @Valid @RequestBody SetItemStatusRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return OrderItemResponse.from(kitchenService.setItemStatus(tenantId, itemId, req.status()));
    }
}
