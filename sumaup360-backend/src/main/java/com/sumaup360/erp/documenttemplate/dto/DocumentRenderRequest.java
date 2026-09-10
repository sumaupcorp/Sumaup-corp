package com.sumaup360.erp.documenttemplate.dto;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Render de PDF con datos reales enviados por el cliente.
 * Si se envia templateId (con companyId), se toma su configuracion; si no, se usan defaults
 * con documentType + printFormat. data trae el contenido del documento.
 */
public record DocumentRenderRequest(
        UUID templateId,
        UUID companyId,
        DocumentType documentType,
        PrintFormat printFormat,
        @NotNull DocumentData data
) {
}
