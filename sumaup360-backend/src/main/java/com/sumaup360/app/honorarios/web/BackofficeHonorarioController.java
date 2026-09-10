package com.sumaup360.app.honorarios.web;

import com.sumaup360.app.honorarios.dto.HonorariosDtos.AttachRequest;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.AttachmentView;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.HonorarioView;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.SetStatusRequest;
import com.sumaup360.app.honorarios.service.BackofficeHonorarioService;
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

/** Backoffice: procesamiento de solicitudes de Recibo por Honorarios. */
@RestController
@RequestMapping("/api/v1/backoffice/honorarios")
@Tag(name = "Backoffice - Honorarios", description = "Solicitudes de recibo por honorarios")
public class BackofficeHonorarioController {

    private final BackofficeHonorarioService service;

    public BackofficeHonorarioController(BackofficeHonorarioService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Lista solicitudes de recibo por honorarios (opcional filtro por estado)")
    public List<HonorarioView> list(@RequestParam(required = false) String estado) {
        return service.list(estado).stream().map(HonorarioView::from).toList();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Cambia el estado de una solicitud (notifica al profesional)")
    public HonorarioView setStatus(@PathVariable UUID id, @Valid @RequestBody SetStatusRequest req) {
        return HonorarioView.from(service.setStatus(id, req.estado(), req.observacion()));
    }

    @PostMapping("/{id}/attach")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Adjunta el PDF del recibo generado")
    public AttachmentView attach(@PathVariable UUID id, @Valid @RequestBody AttachRequest req) {
        UUID staffId = SecurityUtils.currentPrincipal().userId();
        return AttachmentView.from(service.attach(id, req.fileUrl(), req.fileName(), staffId));
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Adjuntos de una solicitud")
    public List<AttachmentView> attachments(@PathVariable UUID id) {
        return service.attachments(id).stream().map(AttachmentView::from).toList();
    }
}
