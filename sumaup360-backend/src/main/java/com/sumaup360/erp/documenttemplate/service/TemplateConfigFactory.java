package com.sumaup360.erp.documenttemplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.erp.documenttemplate.dto.TemplateConfig;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import com.sumaup360.erp.documenttemplate.model.DocumentTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Construye la TemplateConfig de render a partir de una plantilla persistida o de defaults.
 * Los textos de pie pueden venir en footer_config (JSON) de la plantilla.
 */
@Component
public class TemplateConfigFactory {

    private final ObjectMapper objectMapper;

    public TemplateConfigFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TemplateConfig fromTemplate(DocumentTemplate t) {
        Map<String, Object> footer = parse(t.getFooterConfig());
        return TemplateConfig.builder()
                .primaryColor(t.getPrimaryColor())
                .secondaryColor(t.getSecondaryColor())
                .fontFamily(t.getFontFamily())
                .logoUrl(t.getLogoUrl())
                .showLogo(t.isShowLogo())
                .showQr(t.isShowQr())
                .showPaymentInfo(t.isShowPaymentInfo())
                .showSeller(t.isShowSeller())
                .showCustomerAddress(t.isShowCustomerAddress())
                .showBusinessExtraFields(t.isShowBusinessExtraFields())
                .printFormat(t.getPrintFormat().name())
                .footerText(text(footer, "footerText", "Gracias por su compra."))
                .legalMessage(text(footer, "legalMessage", defaultLegal(t.getDocumentType())))
                .commercialMessage(text(footer, "commercialMessage", ""))
                .thankYouMessage(text(footer, "thankYouMessage", "Gracias por su preferencia"))
                .customCss(t.getCustomCss())
                .internalDocument(!t.getDocumentType().isFiscal())
                .build();
    }

    public TemplateConfig defaults(DocumentType documentType, PrintFormat printFormat) {
        return TemplateConfig.builder()
                .primaryColor("#0B5BFF")
                .secondaryColor("#102A4C")
                .fontFamily("Helvetica, Arial, sans-serif")
                .logoUrl(null)
                .showLogo(true)
                .showQr(documentType.isFiscal())
                .showPaymentInfo(true)
                .showSeller(true)
                .showCustomerAddress(true)
                .showBusinessExtraFields(true)
                .printFormat(printFormat.name())
                .footerText("Gracias por su compra.")
                .legalMessage(defaultLegal(documentType))
                .commercialMessage("")
                .thankYouMessage("Gracias por su preferencia")
                .customCss(null)
                .internalDocument(!documentType.isFiscal())
                .build();
    }

    private String defaultLegal(DocumentType type) {
        if (!type.isFiscal()) {
            return "Documento interno - sin valor tributario.";
        }
        // TODO SUNAT: leyenda oficial de representacion impresa del comprobante electronico.
        return "Representacion impresa del comprobante electronico.";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private String text(Map<String, Object> map, String key, String fallback) {
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : fallback;
    }
}
