package com.sumaup360.backoffice.web;

import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.backoffice.dto.BackofficeDtos.AttachFileRequest;
import com.sumaup360.backoffice.dto.BackofficeDtos.DeclareRequest;
import com.sumaup360.backoffice.dto.BackofficeDtos.FileUrlResponse;
import com.sumaup360.backoffice.dto.BackofficeDtos.ProcessReceiptRequest;
import com.sumaup360.backoffice.dto.BackofficeDtos.ReceiptView;
import com.sumaup360.backoffice.dto.BackofficeDtos.RevealSolResponse;
import com.sumaup360.backoffice.dto.BackofficeDtos.UserFiscalRequest;
import com.sumaup360.backoffice.service.BackofficeReceiptService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Backoffice (staff, cross-user): cola de recibos de las personas, su detalle fiscal,
 * historial por usuario y declaracion en SIRE/SUNAT.
 */
@RestController
@RequestMapping("/api/v1/backoffice/receipts")
@Tag(name = "Backoffice - Recibos", description = "Intake, detalle fiscal y declaracion de recibos")
public class BackofficeReceiptController {

    private final BackofficeReceiptService service;

    public BackofficeReceiptController(BackofficeReceiptService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Cola de recibos por estado (default PENDING) (requiere receipt:read)")
    public List<ReceiptView> list(@RequestParam(required = false) ReceiptStatus status) {
        return service.listByStatus(status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Detalle de un recibo con datos del usuario (requiere receipt:read)")
    public ReceiptView detail(@PathVariable UUID id) {
        return service.detail(id);
    }

    @GetMapping("/by-user/{userId}")
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Historial acumulado de recibos de un usuario (requiere receipt:read)")
    public List<ReceiptView> byUser(@PathVariable UUID userId) {
        return service.byUser(userId);
    }

    @PostMapping("/{id}/take")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Toma un recibo para procesarlo (requiere receipt:process)")
    public ReceiptView take(@PathVariable UUID id) {
        return service.take(id, SecurityUtils.currentPrincipal().userId());
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Marca un recibo como procesado (requiere receipt:process)")
    public ReceiptView process(@PathVariable UUID id,
                               @RequestBody(required = false) ProcessReceiptRequest req) {
        return service.process(id, req != null ? req.note() : null);
    }

    @PostMapping("/{id}/observe")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Observa un recibo (devuelve con nota) (requiere receipt:process)")
    public ReceiptView observe(@PathVariable UUID id,
                               @RequestBody(required = false) ProcessReceiptRequest req) {
        return service.observe(id, req != null ? req.note() : null);
    }

    @PostMapping("/{id}/declare-sire")
    @PreAuthorize("hasAuthority('receipt:declare')")
    @Operation(summary = "Marca el recibo como declarado en SIRE (requiere receipt:declare)")
    public ReceiptView declareSire(@PathVariable UUID id, @RequestBody DeclareRequest req) {
        return service.declareSire(id, req.period());
    }

    @PostMapping("/{id}/declare-sunat")
    @PreAuthorize("hasAuthority('receipt:declare')")
    @Operation(summary = "Marca el recibo como declarado en SUNAT (requiere receipt:declare)")
    public ReceiptView declareSunat(@PathVariable UUID id, @RequestBody DeclareRequest req) {
        return service.declareSunat(id, req.period());
    }

    @PostMapping("/{id}/attach")
    @PreAuthorize("hasAuthority('receipt:process')")
    @Operation(summary = "Asocia la ruta del archivo en Storage al recibo (requiere receipt:process)")
    public ReceiptView attach(@PathVariable UUID id, @RequestBody AttachFileRequest req) {
        return service.attachFile(id, req.filePath());
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("hasAuthority('receipt:read')")
    @Operation(summary = "Signed URL de corta duracion para ver el archivo (requiere receipt:read)")
    public FileUrlResponse file(@PathVariable UUID id) {
        return new FileUrlResponse(service.fileUrl(id), 600);
    }

    @PutMapping("/users/{userId}/fiscal")
    @PreAuthorize("hasAuthority('sol:manage')")
    @Operation(summary = "Registra/actualiza datos fiscales del usuario (RUC, Clave SOL) (requiere sol:manage)")
    public void upsertUserFiscal(@PathVariable UUID userId, @RequestBody UserFiscalRequest req) {
        service.upsertUserFiscal(userId, req.ruc(), req.regime(), req.solUser(), req.solPass());
    }

    @PostMapping("/users/{userId}/reveal-sol")
    @PreAuthorize("hasAuthority('sol:reveal')")
    @Operation(summary = "Revela (descifra) la Clave SOL del usuario — auditado (requiere sol:reveal)")
    public RevealSolResponse revealSol(@PathVariable UUID userId) {
        return service.revealSol(userId, SecurityUtils.currentPrincipal().userId());
    }
}
