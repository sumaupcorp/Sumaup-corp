package com.sumaup360.erp.patient.controller;

import com.sumaup360.erp.patient.dto.PatientDtos.CreatePatientRequest;
import com.sumaup360.erp.patient.dto.PatientDtos.PatientResponse;
import com.sumaup360.erp.patient.dto.PatientDtos.UpdatePatientRequest;
import com.sumaup360.erp.patient.service.PatientService;
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

/** Pacientes / mascotas (modulo patients). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp/patients")
@Tag(name = "Pacientes", description = "Pacientes / mascotas asociados a clientes del negocio")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('patient:manage')")
    @Operation(summary = "Registra un paciente / mascota (requiere patient:manage)")
    public PatientResponse create(@Valid @RequestBody CreatePatientRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return patientService.create(tenantId, req);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('patient:read')")
    @Operation(summary = "Lista pacientes, opcionalmente por cliente dueno (requiere patient:read)")
    public List<PatientResponse> list(@RequestParam(required = false) UUID customerId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return patientService.list(tenantId, customerId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:read')")
    @Operation(summary = "Detalle de un paciente (requiere patient:read)")
    public PatientResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return patientService.get(tenantId, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('patient:manage')")
    @Operation(summary = "Actualiza un paciente (requiere patient:manage)")
    public PatientResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePatientRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return patientService.update(tenantId, id, req);
    }
}
