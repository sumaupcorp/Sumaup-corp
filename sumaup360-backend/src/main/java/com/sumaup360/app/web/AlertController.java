package com.sumaup360.app.web;

import com.sumaup360.app.dto.AlertDtos.AlertResponse;
import com.sumaup360.app.dto.AlertDtos.CreateAlertRequest;
import com.sumaup360.app.dto.AlertDtos.UpdateAlertStatusRequest;
import com.sumaup360.app.service.AlertService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Alertas/recordatorios de la persona. */
@RestController
@RequestMapping("/api/v1/app/alerts")
@Tag(name = "Personas - Alertas", description = "Alertas y recordatorios")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una alerta/recordatorio")
    public AlertResponse create(@Valid @RequestBody CreateAlertRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return AlertResponse.from(alertService.create(userId, req));
    }

    @GetMapping
    @Operation(summary = "Lista las alertas de la persona")
    public List<AlertResponse> list() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return alertService.list(userId).stream().map(AlertResponse::from).toList();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambia el estado de una alerta")
    public AlertResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateAlertStatusRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return AlertResponse.from(alertService.updateStatus(id, userId, req.status()));
    }
}
