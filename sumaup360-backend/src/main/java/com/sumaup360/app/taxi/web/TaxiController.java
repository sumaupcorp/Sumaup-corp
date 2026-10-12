package com.sumaup360.app.taxi.web;

import com.sumaup360.app.taxi.dto.TaxiDtos.AttachmentView;
import com.sumaup360.app.taxi.dto.TaxiDtos.CreateCorporateRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.EditMontoRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.QrTokenView;
import com.sumaup360.app.taxi.dto.TaxiDtos.RejectRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.RequestView;
import com.sumaup360.app.taxi.dto.TaxiDtos.UpdateCorporateRequest;
import com.sumaup360.app.taxi.service.TaxiService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(summary = "Token del QR del taxista")
    public QrTokenView qr() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return new QrTokenView(taxiService.getQrToken(userId));
    }

    @GetMapping("/requests")
    @Operation(summary = "Mis solicitudes de comprobante (opcional filtro por origen)")
    public List<RequestView> requests(@RequestParam(required = false) String origen) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return taxiService.myRequests(userId, origen).stream().map(RequestView::from).toList();
    }

    @PostMapping("/corporate-requests")
    @Operation(summary = "Crea una solicitud de factura corporativa")
    public RequestView createCorporateRequest(@Valid @RequestBody CreateCorporateRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return RequestView.from(taxiService.createCorporateRequest(userId, req));
    }

    @PutMapping("/corporate-requests/{id}")
    @Operation(summary = "Edita una solicitud de factura corporativa antes de que backoffice la complete")
    public RequestView updateCorporateRequest(@PathVariable UUID id, @Valid @RequestBody UpdateCorporateRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return RequestView.from(taxiService.updateCorporateRequest(userId, id, req));
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
