package com.sumaup360.app.web;

import com.sumaup360.app.dto.SummaryDtos.SummaryResponse;
import com.sumaup360.app.service.SummaryService;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/** Resumen del mes para el home de la app (ingresos vs gastos y utilidad). */
@RestController
@RequestMapping("/api/v1/app/summary")
@Tag(name = "Personas - Resumen", description = "Resumen financiero mensual")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping
    @Operation(summary = "Resumen del mes (period=YYYY-MM, por defecto el mes actual)")
    public SummaryResponse monthly(@RequestParam(required = false) String period) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        YearMonth ym;
        try {
            ym = (period == null || period.isBlank()) ? YearMonth.now() : YearMonth.parse(period);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Formato de period invalido. Usa YYYY-MM.");
        }
        return summaryService.monthly(userId, ym);
    }
}
