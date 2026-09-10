package com.sumaup360.backoffice.web;

import com.sumaup360.backoffice.dto.OverviewDtos.Overview;
import com.sumaup360.backoffice.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Estadisticas del dashboard del Backoffice. */
@RestController
@RequestMapping("/api/v1/backoffice/overview")
@Tag(name = "Backoffice - Overview", description = "Estadisticas agregadas del ecosistema")
public class BackofficeStatsController {

    private final StatsService statsService;

    public BackofficeStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    @Operation(summary = "Resumen agregado para el dashboard (requiere client:read)")
    public Overview overview() {
        return statsService.overview();
    }
}
