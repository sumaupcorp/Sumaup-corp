package com.sumaup360.app.honorarios.web;

import com.sumaup360.app.honorarios.dto.HonorariosDtos.CreateSuspensionRequest;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.SuspensionView;
import com.sumaup360.app.honorarios.service.SuspensionService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Modulo Servicios Profesionales (Premium): solicitudes de Suspension de 4ta (anual). */
@RestController
@RequestMapping("/api/v1/app/suspensiones")
@Tag(name = "Personas - Servicios Profesionales", description = "Suspension de 4ta categoria")
public class SuspensionController {

    private final SuspensionService service;

    public SuspensionController(SuspensionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Solicita la suspension de 4ta para un año")
    public SuspensionView create(@Valid @RequestBody CreateSuspensionRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return SuspensionView.from(service.create(userId, req.anio()));
    }

    @GetMapping
    @Operation(summary = "Mis solicitudes de suspension de 4ta")
    public List<SuspensionView> mine() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return service.myRequests(userId).stream().map(SuspensionView::from).toList();
    }
}
