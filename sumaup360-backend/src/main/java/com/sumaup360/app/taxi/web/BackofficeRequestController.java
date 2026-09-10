package com.sumaup360.app.taxi.web;

import com.sumaup360.app.taxi.dto.TaxiDtos.AttachRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.AttachmentView;
import com.sumaup360.app.taxi.dto.TaxiDtos.RequestView;
import com.sumaup360.app.taxi.dto.TaxiDtos.SetStatusRequest;
import com.sumaup360.app.taxi.service.BackofficeRequestService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Backoffice: solicitudes de comprobante de taxistas (procesamiento y adjuntos). */
@RestController
@RequestMapping("/api/v1/backoffice/requests")
@Tag(name = "Backoffice - Solicitudes", description = "Solicitudes de comprobante de taxistas")
public class BackofficeRequestController {

    private final BackofficeRequestService service;

    public BackofficeRequestController(BackofficeRequestService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Lista solicitudes (opcional filtro por estado)")
    public List<RequestView> list(@RequestParam(required = false) String estado) {
        return service.list(estado).stream().map(RequestView::from).toList();
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Cambia el estado de una solicitud (notifica al taxista)")
    public RequestView setStatus(@PathVariable UUID id, @Valid @RequestBody SetStatusRequest req) {
        return RequestView.from(service.setStatus(id, req.estado(), req.motivo()));
    }

    @PostMapping("/{id}/attach")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Adjunta un archivo (PDF/XML/CDR) a la solicitud")
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

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Elimina un adjunto de la solicitud")
    public void deleteAttachment(@PathVariable UUID id, @PathVariable UUID attachmentId) {
        service.deleteAttachment(id, attachmentId);
    }
}
