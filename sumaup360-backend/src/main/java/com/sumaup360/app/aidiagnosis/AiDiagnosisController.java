package com.sumaup360.app.aidiagnosis;

import com.sumaup360.app.aidiagnosis.AiDiagnosisDtos.GenerateRequest;
import com.sumaup360.app.aidiagnosis.AiDiagnosisDtos.ReportView;
import com.sumaup360.app.aidiagnosis.AiDiagnosisDtos.StatusView;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Diagnostico tributario con IA. Gratis, de un solo intento por cuenta: el usuario ingresa
 * su DNI/RUC, se consulta SUNAT y la IA recomienda el regimen adecuado.
 */
@RestController
@RequestMapping("/api/v1/app/ai-diagnosis")
@Tag(name = "Personas - Diagnostico IA", description = "Diagnostico tributario generado con IA")
public class AiDiagnosisController {

    private final DiagnosisReportService service;

    public AiDiagnosisController(DiagnosisReportService service) {
        this.service = service;
    }

    @GetMapping("/status")
    @Operation(summary = "Si el usuario tiene su intento disponible y su ultimo diagnostico")
    public StatusView status() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return StatusView.of(service.status(userId));
    }

    @PostMapping("/generate")
    @Operation(summary = "Genera el diagnostico a partir del DNI/RUC (consulta SUNAT + IA)")
    public ReportView generate(@RequestBody GenerateRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ReportView.from(service.generate(userId, req.doc(), req.docType()));
    }

    @GetMapping("/reports")
    @Operation(summary = "Historico de diagnosticos")
    public List<ReportView> reports() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return service.reports(userId).stream().map(ReportView::from).toList();
    }
}
