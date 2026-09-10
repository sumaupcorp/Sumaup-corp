package com.sumaup360.app.peya.web;

import com.sumaup360.app.peya.dto.PeyaDtos.CreateUploadRequest;
import com.sumaup360.app.peya.dto.PeyaDtos.UploadView;
import com.sumaup360.app.peya.service.PeyaService;
import com.sumaup360.app.taxi.dto.TaxiDtos.AttachmentView;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Modulo Peya (Premium): carga del PDF mensual de ventas y seguimiento. */
@RestController
@RequestMapping("/api/v1/app/peya")
@Tag(name = "Personas - Peya", description = "Carga mensual de ventas Peya")
public class PeyaController {

    private final PeyaService peyaService;

    public PeyaController(PeyaService peyaService) {
        this.peyaService = peyaService;
    }

    @PostMapping("/uploads")
    @Operation(summary = "Sube el PDF mensual de ventas de Peya")
    public UploadView upload(@Valid @RequestBody CreateUploadRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return UploadView.from(peyaService.create(userId, req.periodo(), req.pdfUrl(), req.observacion()));
    }

    @GetMapping("/uploads")
    @Operation(summary = "Mis cargas mensuales de Peya")
    public List<UploadView> uploads() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return peyaService.myUploads(userId).stream().map(UploadView::from).toList();
    }

    @GetMapping("/uploads/{id}/files")
    @Operation(summary = "Archivos resultado (reporte/declaracion) de una carga")
    public List<AttachmentView> files(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return peyaService.myFiles(userId, id).stream().map(AttachmentView::from).toList();
    }
}
