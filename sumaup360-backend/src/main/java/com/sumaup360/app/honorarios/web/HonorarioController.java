package com.sumaup360.app.honorarios.web;

import com.sumaup360.app.honorarios.dto.HonorariosDtos.CreateHonorarioRequest;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.HonorarioView;
import com.sumaup360.app.honorarios.service.HonorarioService;
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

/** Modulo Servicios Profesionales (Premium): solicitudes de Recibo por Honorarios. */
@RestController
@RequestMapping("/api/v1/app/honorarios")
@Tag(name = "Personas - Servicios Profesionales", description = "Recibos por honorarios")
public class HonorarioController {

    private final HonorarioService service;

    public HonorarioController(HonorarioService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Solicita la generacion de un Recibo por Honorarios")
    public HonorarioView create(@Valid @RequestBody CreateHonorarioRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return HonorarioView.from(service.create(userId, req));
    }

    @GetMapping
    @Operation(summary = "Mis solicitudes de recibo por honorarios")
    public List<HonorarioView> mine() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return service.myRequests(userId).stream().map(HonorarioView::from).toList();
    }
}
