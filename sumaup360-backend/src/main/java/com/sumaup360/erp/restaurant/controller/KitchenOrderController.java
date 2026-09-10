package com.sumaup360.erp.restaurant.controller;

import com.sumaup360.erp.restaurant.domain.KitchenOrder;
import com.sumaup360.erp.restaurant.dto.OrderDtos.AddItemRequest;
import com.sumaup360.erp.restaurant.dto.OrderDtos.BillOrderRequest;
import com.sumaup360.erp.restaurant.dto.OrderDtos.CreateOrderRequest;
import com.sumaup360.erp.restaurant.dto.OrderDtos.OrderItemResponse;
import com.sumaup360.erp.restaurant.dto.OrderDtos.OrderResponse;
import com.sumaup360.erp.restaurant.enums.OrderStatus;
import com.sumaup360.erp.restaurant.service.KitchenOrderService;
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

/** Comandas del restaurante: crear, agregar items, enviar a cocina, cobrar. */
@RestController
@RequestMapping("/api/v1/erp/restaurant/orders")
@Tag(name = "Restaurante - Comandas", description = "Ciclo mesa -> comanda -> cocina -> cobro")
public class KitchenOrderController {

    private final KitchenOrderService orderService;

    public KitchenOrderController(KitchenOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('order:create')")
    @Operation(summary = "Crea una comanda (requiere order:create)")
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        KitchenOrder o = orderService.createOrder(tenantId, req.branchId(), req.type(),
                req.tableId(), req.notes(), userId);
        return OrderResponse.from(o, orderService.listItems(o.getId()));
    }

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('order:create')")
    @Operation(summary = "Agrega un item a la comanda (requiere order:create)")
    public OrderItemResponse addItem(@PathVariable UUID id, @Valid @RequestBody AddItemRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return OrderItemResponse.from(orderService.addItem(
                tenantId, id, req.productId(), req.quantity(), req.notes(), req.station()));
    }

    @PostMapping("/{id}/send-to-kitchen")
    @PreAuthorize("hasAuthority('order:manage')")
    @Operation(summary = "Envia la comanda a cocina (requiere order:manage)")
    public OrderResponse sendToKitchen(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        KitchenOrder o = orderService.sendToKitchen(tenantId, id);
        return OrderResponse.from(o, orderService.listItems(o.getId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:manage')")
    @Operation(summary = "Cancela la comanda (requiere order:manage)")
    public OrderResponse cancel(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        KitchenOrder o = orderService.cancelOrder(tenantId, id);
        return OrderResponse.from(o, orderService.listItems(o.getId()));
    }

    @PostMapping("/{id}/bill")
    @PreAuthorize("hasAuthority('order:manage')")
    @Operation(summary = "Cobra la comanda: genera venta y libera la mesa (requiere order:manage)")
    public OrderResponse bill(@PathVariable UUID id, @RequestBody(required = false) BillOrderRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        UUID customerId = req != null ? req.customerId() : null;
        KitchenOrder o = orderService.billOrder(tenantId, id, customerId, userId);
        return OrderResponse.from(o, orderService.listItems(o.getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('order:read')")
    @Operation(summary = "Lista comandas de una sucursal (requiere order:read)")
    public List<OrderResponse> list(@RequestParam UUID branchId,
                                    @RequestParam(required = false) OrderStatus status) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return orderService.listOrders(tenantId, branchId, status).stream()
                .map(o -> OrderResponse.from(o, orderService.listItems(o.getId())))
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('order:read')")
    @Operation(summary = "Detalle de comanda con items (requiere order:read)")
    public OrderResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        KitchenOrder o = orderService.getOrder(tenantId, id);
        return OrderResponse.from(o, orderService.listItems(o.getId()));
    }
}
