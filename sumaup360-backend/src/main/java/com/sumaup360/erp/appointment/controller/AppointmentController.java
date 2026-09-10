package com.sumaup360.erp.appointment.controller;

import com.sumaup360.erp.appointment.dto.AppointmentDtos.AppointmentResponse;
import com.sumaup360.erp.appointment.dto.AppointmentDtos.CreateAppointmentRequest;
import com.sumaup360.erp.appointment.dto.AppointmentDtos.UpdateAppointmentRequest;
import com.sumaup360.erp.appointment.dto.AppointmentDtos.UpdateAppointmentStatusRequest;
import com.sumaup360.erp.appointment.service.AppointmentService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Citas (modulo appointments). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp/appointments")
@Tag(name = "Citas", description = "Agenda de citas por sucursal")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('appointment:manage')")
    @Operation(summary = "Crea una cita (requiere appointment:manage)")
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return appointmentService.create(tenantId, req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    @Operation(summary = "Agenda de citas entre fechas (requiere appointment:read)")
    public List<AppointmentResponse> list(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return appointmentService.list(tenantId, branchId, from, to);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('appointment:read')")
    @Operation(summary = "Detalle de una cita (requiere appointment:read)")
    public AppointmentResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return appointmentService.get(tenantId, id);
    }

    @GetMapping("/ticket/{code}")
    @PreAuthorize("hasAuthority('appointment:read')")
    @Operation(summary = "Busca una cita por codigo de ticket (requiere appointment:read)")
    public AppointmentResponse getByTicket(@PathVariable String code) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return appointmentService.getByTicket(tenantId, code);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('appointment:manage')")
    @Operation(summary = "Reprograma o edita una cita (requiere appointment:manage)")
    public AppointmentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAppointmentRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return appointmentService.update(tenantId, id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('appointment:manage')")
    @Operation(summary = "Cambia el estado de una cita (requiere appointment:manage)")
    public AppointmentResponse updateStatus(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateAppointmentStatusRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return appointmentService.updateStatus(tenantId, id, req.status());
    }
}
