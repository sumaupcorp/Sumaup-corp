package com.sumaup360.app.honorarios.web;

import com.sumaup360.app.honorarios.dto.HonorariosDtos.AttachRequest;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.AttachmentView;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.SetStatusRequest;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.SuspensionView;
import com.sumaup360.app.honorarios.service.BackofficeSuspensionService;
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

/** Backoffice: tramitacion de solicitudes de Suspension de 4ta categoria. */
@RestController
@RequestMapping("/api/v1/backoffice/suspensiones")
@Tag(name = "Backoffice - Suspension 4ta", description = "Solicitudes de suspension de 4ta")
public class BackofficeSuspensionController {

    private final BackofficeSuspensionService service;

    public BackofficeSuspensionController(BackofficeSuspensionService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Lista solicitudes de suspension (opcional filtro por estado)")
    public List<SuspensionView> list(@RequestParam(required = false) String estado) {
        return service.list(estado).stream().map(SuspensionView::from).toList();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Cambia el estado de una solicitud (notifica al profesional)")
    public SuspensionView setStatus(@PathVariable UUID id, @Valid @RequestBody SetStatusRequest req) {
        return SuspensionView.from(service.setStatus(id, req.estado(), req.observacion()));
    }

    @PostMapping("/{id}/attach")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Adjunta la constancia de suspension tramitada")
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
