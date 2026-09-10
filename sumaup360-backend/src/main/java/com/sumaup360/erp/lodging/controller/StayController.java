package com.sumaup360.erp.lodging.controller;

import com.sumaup360.common.web.PageResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.AddChargeRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.ChargeResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CheckInRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CheckOutRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CreateStayRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.StayResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateStayRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateStayStatusRequest;
import com.sumaup360.erp.lodging.enums.StayStatus;
import com.sumaup360.erp.lodging.service.StayService;
import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.BranchAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import java.util.UUID;

/**
 * Estadias / reservas de hospedaje. Multi-tenant + RBAC; las operaciones de sede
 * (reservar, check-in/out, cargos) respetan la sede asignada del trabajador.
 */
@RestController
@RequestMapping("/api/v1/erp/stays")
@Tag(name = "Hospedaje - Estadias", description = "Reservas, check-in/check-out y cargos")
public class StayController {

    private final StayService stayService;
    private final BranchAccessService branchAccessService;

    public StayController(StayService stayService, BranchAccessService branchAccessService) {
        this.stayService = stayService;
        this.branchAccessService = branchAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Crea una reserva; valida solape de fechas (requiere lodging:manage)")
    public StayResponse create(@Valid @RequestBody CreateStayRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        branchAccessService.assertCanOperate(tenantId, userId, req.branchId());
        return stayService.create(tenantId, req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Reservas paginadas por empresa o sucursal (requiere lodging:read)")
    public PageResponse<StayResponse> list(@RequestParam(required = false) UUID companyId,
                                           @RequestParam(required = false) UUID branchId,
                                           @RequestParam(required = false) StayStatus status,
                                           @RequestParam(required = false) Integer page,
                                           @RequestParam(required = false) Integer size) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        int p = PageResponse.sanitizePage(page);
        int s = PageResponse.sanitizeSize(size, 20, 100);
        return PageResponse.from(stayService.search(tenantId, companyId, branchId, status, p, s),
                x -> x);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Detalle de una estadia con huespedes y cargos (requiere lodging:read)")
    public StayResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return stayService.get(tenantId, id);
    }

    @GetMapping("/ticket/{code}")
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Busca una reserva por codigo de ticket (requiere lodging:read)")
    public StayResponse getByTicket(@PathVariable String code) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return stayService.getByTicket(tenantId, code);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Reprograma o edita una reserva pendiente (requiere lodging:manage)")
    public StayResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateStayRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return stayService.update(tenantId, id, req);
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Check-in: registra huespedes y ocupa la habitacion (requiere lodging:manage)")
    public StayResponse checkIn(@PathVariable UUID id, @Valid @RequestBody(required = false) CheckInRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        StayResponse stay = stayService.get(tenantId, id);
        branchAccessService.assertCanOperate(tenantId, userId, stay.branchId());
        return stayService.checkIn(tenantId, id, req);
    }

    @PostMapping("/{id}/charges")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Agrega un cargo a la habitacion (requiere lodging:manage)")
    public ChargeResponse addCharge(@PathVariable UUID id, @Valid @RequestBody AddChargeRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        StayResponse stay = stayService.get(tenantId, id);
        branchAccessService.assertCanOperate(tenantId, userId, stay.branchId());
        return stayService.addCharge(tenantId, id, req, userId);
    }

    @DeleteMapping("/{id}/charges/{chargeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Quita un cargo antes del check-out (requiere lodging:manage)")
    public void removeCharge(@PathVariable UUID id, @PathVariable UUID chargeId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        stayService.removeCharge(tenantId, id, chargeId);
    }

    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Check-out: liquida noches y cargos como venta POS, exige caja abierta (requiere lodging:manage)")
    public StayResponse checkOut(@PathVariable UUID id,
                                 @Valid @RequestBody(required = false) CheckOutRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        StayResponse stay = stayService.get(tenantId, id);
        branchAccessService.assertCanOperate(tenantId, userId, stay.branchId());
        return stayService.checkOut(tenantId, id, req, userId);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Cancela o marca no show una reserva pendiente (requiere lodging:manage)")
    public StayResponse updateStatus(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateStayStatusRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return stayService.updateStatus(tenantId, id, req);
    }
}
