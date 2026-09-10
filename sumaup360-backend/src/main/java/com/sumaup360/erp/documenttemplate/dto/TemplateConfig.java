package com.sumaup360.erp.documenttemplate.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuracion visual aplicada al render (colores, flags y textos).
 * Modelo para Thymeleaf (getters). Se construye desde la plantilla o desde defaults.
 */
@Getter
@Setter
@Builder
public class TemplateConfig {

    private String primaryColor;
    private String secondaryColor;
    private String fontFamily;
    private String logoUrl;
    private boolean showLogo;
    private boolean showQr;
    private boolean showPaymentInfo;
    private boolean showSeller;
    private boolean showCustomerAddress;
    private boolean showBusinessExtraFields;
    private String printFormat;       // A4 / THERMAL_80MM / THERMAL_58MM
    private String footerText;
    private String legalMessage;
    private String commercialMessage;
    private String thankYouMessage;
    private String customCss;
    /** Marca documento interno (comanda/precuenta/nota interna): sin valor tributario. */
    private boolean internalDocument;
}
