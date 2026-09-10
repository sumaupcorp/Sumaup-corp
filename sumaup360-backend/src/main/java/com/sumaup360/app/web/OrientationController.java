package com.sumaup360.app.web;

import com.sumaup360.app.dto.OrientationDtos.AnalyzeRequest;
import com.sumaup360.app.dto.OrientationDtos.OrientationResult;
import com.sumaup360.app.dto.OrientationDtos.OrientationStatusResponse;
import com.sumaup360.app.service.OrientationService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Orientacion tributaria del onboarding (usuarios sin RUC). Linea Personas: scopeado por
 * usuario, sin permiso especial (solo autenticado), igual que /app/profile/me.
 */
@RestController
@RequestMapping("/api/v1/app/orientation")
@Tag(name = "Personas - Orientacion", description = "Orientacion tributaria del onboarding (sin RUC)")
public class OrientationController {

    private final OrientationService service;

    public OrientationController(OrientationService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    @Operation(summary = "Analiza las respuestas del cuestionario y recomienda inscripcion/regimen")
    public OrientationResult analyze(@Valid @RequestBody AnalyzeRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return service.analyze(userId, req);
    }

    @GetMapping
    @Operation(summary = "Estado y resultado guardado de la orientacion de la persona")
    public OrientationStatusResponse status() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return service.status(userId);
    }
}
