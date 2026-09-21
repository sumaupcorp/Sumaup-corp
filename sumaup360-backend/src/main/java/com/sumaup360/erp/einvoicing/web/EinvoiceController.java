package com.sumaup360.erp.einvoicing.web;

import com.sumaup360.erp.einvoicing.service.EinvoiceService;
import com.sumaup360.erp.einvoicing.web.EinvoiceDtos.ConfigResponse;
import com.sumaup360.erp.einvoicing.web.EinvoiceDtos.DocumentHistoryResponse;
import com.sumaup360.erp.einvoicing.web.EinvoiceDtos.EmitResponse;
import com.sumaup360.erp.einvoicing.web.EinvoiceDtos.SaveConfigRequest;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Facturacion electronica (NubeFact) del tenant del contexto. */
@RestController
@RequestMapping("/api/v1/erp/einvoicing")
@Tag(name = "Facturacion electronica", description = "Credenciales NubeFact y emision de comprobantes ante SUNAT")
public class EinvoiceController {

    private final EinvoiceService service;

    public EinvoiceController(EinvoiceService service) {
        this.service = service;
    }

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('einvoice:config')")
    @Operation(summary = "Config NubeFact de una empresa (el token no se devuelve; requiere einvoice:config)")
    public ConfigResponse getConfig(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return ConfigResponse.from(companyId, service.getConfig(tenantId, companyId));
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('einvoice:config')")
    @Operation(summary = "Guarda las credenciales NubeFact (ruta + token cifrado) de una empresa (requiere einvoice:config)")
    public ConfigResponse saveConfig(@Valid @RequestBody SaveConfigRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return ConfigResponse.from(req.companyId(),
                service.saveConfig(tenantId, req.companyId(), req.ruta(), req.token(),
                        Boolean.TRUE.equals(req.enabled())));
    }

    @PostMapping("/sales/{saleId}/emit")
    @PreAuthorize("hasAuthority('einvoice:emit')")
    @Operation(summary = "Emite el comprobante electronico (boleta/factura) de una venta via NubeFact (requiere einvoice:emit)")
    public EmitResponse emit(@PathVariable UUID saleId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return EmitResponse.from(service.emitFromSale(tenantId, saleId));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAnyAuthority('einvoice:emit','sale:read')")
    @Operation(summary = "Historial de comprobantes electronicos emitidos de una empresa (requiere sale:read)")
    public java.util.List<DocumentHistoryResponse> documents(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return service.listDocuments(tenantId, companyId).stream()
                .map(DocumentHistoryResponse::from).toList();
    }
}
