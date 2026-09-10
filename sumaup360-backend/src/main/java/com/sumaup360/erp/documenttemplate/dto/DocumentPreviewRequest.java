package com.sumaup360.erp.documenttemplate.dto;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;

import java.util.UUID;

/**
 * Preview con datos de EJEMPLO. Si se envia templateId, usa su configuracion (requiere
 * companyId para validar el tenant); si no, usa una configuracion por defecto.
 * businessType (restaurant|hardware|pharmacy|delivery) decide los campos opcionales de ejemplo.
 * config (opcional) trae la configuracion SIN GUARDAR del editor: pisa la resuelta para
 * que la vista previa se actualice en vivo mientras el usuario edita.
 */
public record DocumentPreviewRequest(
        UUID templateId,
        UUID companyId,
        DocumentType documentType,
        PrintFormat printFormat,
        String businessType,
        InlineConfig config
) {

    /** Solo se aplican los campos no nulos. */
    public record InlineConfig(
            String primaryColor,
            String secondaryColor,
            String logoUrl,
            Boolean showLogo,
            Boolean showQr,
            Boolean showPaymentInfo,
            Boolean showSeller,
            Boolean showCustomerAddress,
            Boolean showBusinessExtraFields,
            String footerText,
            String legalMessage,
            String commercialMessage,
            String thankYouMessage
    ) {
    }
}
