package com.sumaup360.app.taxi.web;

import com.sumaup360.app.taxi.dto.TaxiDtos.AttachmentView;
import com.sumaup360.app.taxi.dto.TaxiDtos.EditMontoRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.QrTokenView;
import com.sumaup360.app.taxi.dto.TaxiDtos.RejectRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.RequestView;
import com.sumaup360.app.taxi.service.TaxiService;
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

/** Modulo Taxista (Premium): QR y gestion de solicitudes de comprobante. */
@RestController
@RequestMapping("/api/v1/app/taxi")
@Tag(name = "Personas - Taxista", description = "QR y solicitudes de comprobante del taxista")
public class TaxiController {

    private final TaxiService taxiService;

    public TaxiController(TaxiService taxiService) {
        this.taxiService = taxiService;
    }

    @GetMapping("/qr")
    @Operation(summary = "Token del QR del taxista (lo crea si no existe)")
    public QrTokenView qr() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return new QrTokenView(taxiService.getOrCreateQrToken(userId));
    }

    @PostMapping("/qr/regenerate")
    @Operation(summary = "Regenera el token del QR (invalida el anterior)")
    public QrTokenView regenerateQr() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return new QrTokenView(taxiService.regenerateQrToken(userId));
    }

    @GetMapping("/requests")
    @Operation(summary = "Mis solicitudes de comprobante")
    public List<RequestView> requests() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return taxiService.myRequests(userId).stream().map(RequestView::from).toList();
    }

    @GetMapping("/requests/{id}/attachments")
    @Operation(summary = "Adjuntos de una de mis solicitudes (comprobantes generados)")
    public List<AttachmentView> attachments(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return taxiService.myRequestAttachments(userId, id).stream().map(AttachmentView::from).toList();
    }

    @PostMapping("/requests/{id}/confirm")
    @Operation(summary = "Confirma una solicitud")
    public RequestView confirm(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return RequestView.from(taxiService.confirm(userId, id));
    }

    @PostMapping("/requests/{id}/edit")
    @Operation(summary = "Edita el monto de una solicitud y confirma")
    public RequestView edit(@PathVariable UUID id, @Valid @RequestBody EditMontoRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return RequestView.from(taxiService.editMonto(userId, id, req.monto(), req.motivo()));
    }

    @PostMapping("/requests/{id}/reject")
    @Operation(summary = "Rechaza una solicitud con motivo")
    public RequestView reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return RequestView.from(taxiService.reject(userId, id, req.motivo()));
    }
}
