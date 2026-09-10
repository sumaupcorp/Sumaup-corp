package com.sumaup360.erp.documenttemplate.dto;

import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import com.sumaup360.erp.documenttemplate.model.DocumentTemplate;

import java.util.UUID;

public record DocumentTemplateResponse(
        UUID id,
        UUID companyId,
        UUID branchId,
        UUID businessTypeId,
        DocumentType documentType,
        PrintFormat printFormat,
        String templateName,
        String templateCode,
        boolean isDefault,
        boolean active,
        String primaryColor,
        String secondaryColor,
        String fontFamily,
        String logoUrl,
        boolean showLogo,
        boolean showQr,
        boolean showPaymentInfo,
        boolean showSeller,
        boolean showCustomerAddress,
        boolean showBusinessExtraFields,
        String headerConfig,
        String bodyConfig,
        String footerConfig,
        String customCss
) {
    public static DocumentTemplateResponse from(DocumentTemplate t) {
        return new DocumentTemplateResponse(
                t.getId(), t.getCompanyId(), t.getBranchId(), t.getBusinessTypeId(),
                t.getDocumentType(), t.getPrintFormat(), t.getTemplateName(), t.getTemplateCode(),
                t.isDefaultTemplate(), t.isActive(), t.getPrimaryColor(), t.getSecondaryColor(),
                t.getFontFamily(), t.getLogoUrl(), t.isShowLogo(), t.isShowQr(),
                t.isShowPaymentInfo(), t.isShowSeller(), t.isShowCustomerAddress(),
                t.isShowBusinessExtraFields(), t.getHeaderConfig(), t.getBodyConfig(),
                t.getFooterConfig(), t.getCustomCss());
    }
}
