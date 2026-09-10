package com.sumaup360.app.peya.web;

import com.sumaup360.app.peya.dto.PeyaDtos.PeyaAttachRequest;
import com.sumaup360.app.peya.dto.PeyaDtos.SetNpsRequest;
import com.sumaup360.app.peya.dto.PeyaDtos.SetPeyaStatusRequest;
import com.sumaup360.app.peya.dto.PeyaDtos.UploadView;
import com.sumaup360.app.peya.service.BackofficePeyaService;
import com.sumaup360.app.taxi.dto.TaxiDtos.AttachmentView;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Backoffice: cargas Peya (procesar, NPS, adjuntar reporte). */
@RestController
@RequestMapping("/api/v1/backoffice/peya")
@Tag(name = "Backoffice - Peya", description = "Cargas mensuales de Peya")
public class BackofficePeyaController {

    private final BackofficePeyaService service;

    public BackofficePeyaController(BackofficePeyaService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Lista cargas Peya (opcional filtro por estado)")
    public List<UploadView> list(@RequestParam(required = false) String estado) {
        return service.list(estado).stream().map(UploadView::from).toList();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Cambia el estado de una carga (notifica al usuario)")
    public UploadView setStatus(@PathVariable UUID id, @Valid @RequestBody SetPeyaStatusRequest req) {
        return UploadView.from(service.setStatus(id, req.estado(), req.observacion()));
    }

    @PostMapping("/{id}/nps")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Registra el codigo NPS y notifica al usuario")
    public UploadView setNps(@PathVariable UUID id, @Valid @RequestBody SetNpsRequest req) {
        return UploadView.from(service.setNps(id, req.codigoNps()));
    }

    @PostMapping("/{id}/attach")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Adjunta el reporte/declaracion de la carga")
    public AttachmentView attach(@PathVariable UUID id, @Valid @RequestBody PeyaAttachRequest req) {
        UUID staffId = SecurityUtils.currentPrincipal().userId();
        return AttachmentView.from(service.attach(id, req.fileUrl(), req.fileName(), staffId));
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Archivos de una carga Peya")
    public List<AttachmentView> attachments(@PathVariable UUID id) {
        return service.attachments(id).stream().map(AttachmentView::from).toList();
    }
}
