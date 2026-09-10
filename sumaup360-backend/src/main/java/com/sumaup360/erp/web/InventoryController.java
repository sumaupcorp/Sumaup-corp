package com.sumaup360.erp.web;

import com.sumaup360.erp.domain.InventoryStock;
import com.sumaup360.erp.service.InventoryService;
import com.sumaup360.erp.web.dto.InventoryDtos.AdjustStockRequest;
import com.sumaup360.erp.web.dto.InventoryDtos.MovementResponse;
import com.sumaup360.erp.web.dto.InventoryDtos.StockResponse;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Inventario del tenant del contexto: consulta de stock y ajustes. */
@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventario", description = "Stock por producto y sucursal")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "Consulta el stock de un producto en una sucursal (requiere inventory:read)")
    public StockResponse stock(@RequestParam UUID productId, @RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return new StockResponse(productId, branchId,
                inventoryService.getQuantity(tenantId, productId, branchId));
    }

    @GetMapping("/stock/by-branch")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "Stock de todos los productos de una sucursal (requiere inventory:read)")
    public List<StockResponse> stockByBranch(@RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return inventoryService.listByBranch(tenantId, branchId).stream()
                .map(s -> new StockResponse(s.getProductId(), s.getBranchId(), s.getQuantity()))
                .toList();
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "Kardex paginado de una sucursal, mas reciente primero (requiere inventory:read)")
    public com.sumaup360.common.web.PageResponse<MovementResponse> movements(
            @RequestParam UUID branchId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        int p = com.sumaup360.common.web.PageResponse.sanitizePage(page);
        int s = com.sumaup360.common.web.PageResponse.sanitizeSize(size, 10, 100);
        return com.sumaup360.common.web.PageResponse.from(
                inventoryService.pageMovements(tenantId, branchId, p, s), MovementResponse::from);
    }

    @GetMapping("/flow/weekly")
    @PreAuthorize("hasAuthority('inventory:read')")
    @Operation(summary = "Flujo de los ultimos 7 dias: entradas vs salidas por dia (requiere inventory:read)")
    public List<com.sumaup360.erp.web.dto.InventoryDtos.DailyFlowResponse> weeklyFlow(
            @RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return inventoryService.weeklyFlow(tenantId, branchId).stream()
                .map(f -> new com.sumaup360.erp.web.dto.InventoryDtos.DailyFlowResponse(
                        f.date(), f.inQty(), f.outQty()))
                .toList();
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('inventory:adjust')")
    @Operation(summary = "Ajusta el stock (delta con signo) (requiere inventory:adjust)")
    public StockResponse adjust(@Valid @RequestBody AdjustStockRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        InventoryStock stock = inventoryService.adjust(
                tenantId, req.productId(), req.branchId(), req.quantity(), req.reason(), userId);
        return new StockResponse(stock.getProductId(), stock.getBranchId(), stock.getQuantity());
    }
}
