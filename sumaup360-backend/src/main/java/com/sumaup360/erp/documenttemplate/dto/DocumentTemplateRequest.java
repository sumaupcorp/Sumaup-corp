package com.sumaup360.erp.documenttemplate.dto;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Crear/actualizar una plantilla. company_id se valida contra el tenant del contexto. */
public record DocumentTemplateRequest(
        @NotNull UUID companyId,
        UUID branchId,
        UUID businessTypeId,
        @NotNull DocumentType documentType,
        @NotNull PrintFormat printFormat,
        @NotBlank @Size(max = 160) String templateName,
        @NotBlank @Size(max = 60) String templateCode,
        @Size(max = 20) String primaryColor,
        @Size(max = 20) String secondaryColor,
        @Size(max = 80) String fontFamily,
        @Size(max = 500) String logoUrl,
        Boolean showLogo,
        Boolean showQr,
        Boolean showPaymentInfo,
        Boolean showSeller,
        Boolean showCustomerAddress,
        Boolean showBusinessExtraFields,
        String headerConfig,
        String bodyConfig,
        String footerConfig,
        String customCss
) {
}
