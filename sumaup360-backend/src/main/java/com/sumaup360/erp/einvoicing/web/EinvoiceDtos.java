package com.sumaup360.erp.einvoicing.web;

import com.sumaup360.erp.einvoicing.model.CompanyEinvoicingConfig;
import com.sumaup360.erp.einvoicing.service.EinvoiceService;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** DTOs de facturacion electronica (config + emision). */
public final class EinvoiceDtos {

    private EinvoiceDtos() {
    }

    /** Guardar/actualizar credenciales NubeFact de una empresa. El token va en claro solo aqui. */
    public record SaveConfigRequest(@NotNull UUID companyId, String ruta, String token, Boolean enabled) {
    }

    /** Config de la empresa (el token NUNCA se devuelve; solo si esta presente). */
    public record ConfigResponse(UUID companyId, String provider, String ruta, boolean enabled, boolean hasToken) {
        public static ConfigResponse from(UUID companyId, CompanyEinvoicingConfig c) {
            if (c == null) {
                return new ConfigResponse(companyId, "NUBEFACT", null, false, false);
            }
            return new ConfigResponse(companyId, c.getProvider(), c.getNubefactRuta(), c.isEnabled(),
                    c.getNubefactTokenEnc() != null && !c.getNubefactTokenEnc().isBlank());
        }
    }

    /** Resultado de emitir un comprobante electronico. */
    public record EmitResponse(String fullNumber, String documentType, String sunatStatus,
                               String pdfUrl, String xmlUrl, String cdrUrl, String qrValue,
                               String hashValue, boolean accepted, String message) {
        public static EmitResponse from(EinvoiceService.EmitResult r) {
            return new EmitResponse(r.fullNumber(), r.documentType(), r.sunatStatus(), r.pdfUrl(),
                    r.xmlUrl(), r.cdrUrl(), r.qrValue(), r.hashValue(), r.accepted(), r.message());
        }
    }
}
