package com.sumaup360.erp.web;

import com.sumaup360.erp.service.CashSessionService;
import com.sumaup360.erp.web.dto.CashSessionDtos.CashMovementRequest;
import com.sumaup360.erp.web.dto.CashSessionDtos.CashMovementResponse;
import com.sumaup360.erp.web.dto.CashSessionDtos.CashSessionResponse;
import com.sumaup360.erp.web.dto.CashSessionDtos.CashSummaryResponse;
import com.sumaup360.erp.web.dto.CashSessionDtos.CloseCashRequest;
import com.sumaup360.erp.web.dto.CashSessionDtos.OpenCashRequest;
import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.BranchAccessService;
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

import java.util.UUID;

/** Caja / POS del tenant del contexto: apertura, movimientos de efectivo y cierre con arqueo. */
@RestController
@RequestMapping("/api/v1/cash-sessions")
@Tag(name = "Caja / POS", description = "Apertura, movimientos de efectivo y cierre con arqueo")
public class CashSessionController {

    private final CashSessionService cashSessionService;
    private final BranchAccessService branchAccessService;

    public CashSessionController(CashSessionService cashSessionService,
                                 BranchAccessService branchAccessService) {
        this.cashSessionService = cashSessionService;
        this.branchAccessService = branchAccessService;
    }

    @PostMapping("/open")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos:operate')")
    @Operation(summary = "Abre la caja de una sucursal con su fondo inicial (requiere pos:operate)")
    public CashSessionResponse open(@Valid @RequestBody OpenCashRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        branchAccessService.assertCanOperate(tenantId, userId, req.branchId());
        return CashSessionResponse.from(
                cashSessionService.open(tenantId, req.branchId(), req.openingAmount(), userId));
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('pos:operate')")
    @Operation(summary = "Registra un ingreso o salida de efectivo en la caja abierta (requiere pos:operate)")
    public CashMovementResponse addMovement(@PathVariable UUID id,
                                            @Valid @RequestBody CashMovementRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return CashMovementResponse.from(cashSessionService.addMovement(
                tenantId, id, req.type(), req.category(), req.concept(), req.amount(), userId));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('pos:read')")
    @Operation(summary = "Resumen de la caja: ventas por metodo, movimientos y efectivo esperado (requiere pos:read)")
    public CashSummaryResponse summary(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return CashSummaryResponse.from(cashSessionService.summary(tenantId, id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('pos:operate')")
    @Operation(summary = "Cierra la caja con arqueo: contado vs esperado y diferencia (requiere pos:operate)")
    public CashSessionResponse close(@PathVariable UUID id, @Valid @RequestBody CloseCashRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return CashSessionResponse.from(
                cashSessionService.close(tenantId, id, req.countedAmount(), req.notes(), userId));
    }

    @GetMapping("/open")
    @PreAuthorize("hasAuthority('pos:read')")
    @Operation(summary = "Obtiene la caja abierta de una sucursal (requiere pos:read)")
    public CashSessionResponse current(@RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return CashSessionResponse.from(cashSessionService.getOpenByBranch(tenantId, branchId));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('pos:read')")
    @Operation(summary = "Historial paginado de cierres de caja con su arqueo (requiere pos:read)")
    public com.sumaup360.common.web.PageResponse<com.sumaup360.erp.web.dto.CashSessionDtos.CashClosureResponse> history(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        int p = com.sumaup360.common.web.PageResponse.sanitizePage(page);
        int s = com.sumaup360.common.web.PageResponse.sanitizeSize(size, 10, 50);
        return com.sumaup360.common.web.PageResponse.from(
                cashSessionService.history(tenantId, branchId, p, s), r -> r);
    }
}
