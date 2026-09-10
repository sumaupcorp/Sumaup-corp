package com.sumaup360.erp.prescription.controller;

import com.sumaup360.erp.prescription.dto.PrescriptionDtos.CreatePrescriptionRequest;
import com.sumaup360.erp.prescription.dto.PrescriptionDtos.PrescriptionResponse;
import com.sumaup360.erp.prescription.service.PrescriptionService;
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

/** Recetas medicas (modulo prescription). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp/prescriptions")
@Tag(name = "Recetas", description = "Registro de recetas medicas de la farmacia")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('prescription:manage')")
    @Operation(summary = "Registra una receta (requiere prescription:manage)")
    public PrescriptionResponse create(@Valid @RequestBody CreatePrescriptionRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return prescriptionService.create(tenantId, req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('prescription:read')")
    @Operation(summary = "Lista recetas, opcionalmente por sucursal (requiere prescription:read)")
    public List<PrescriptionResponse> list(@RequestParam(required = false) UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return prescriptionService.list(tenantId, branchId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('prescription:read')")
    @Operation(summary = "Detalle de una receta (requiere prescription:read)")
    public PrescriptionResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return prescriptionService.get(tenantId, id);
    }
}
