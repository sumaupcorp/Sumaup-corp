package com.sumaup360.erp.documenttemplate.enums;

/** Formatos de impresion soportados. Cada uno usa su plantilla HTML base. */
public enum PrintFormat {
    A4("documents/document_a4"),
    THERMAL_80MM("documents/document_thermal80"),
    THERMAL_58MM("documents/document_thermal58");

    private final String templateName;

    PrintFormat(String templateName) {
        this.templateName = templateName;
    }

    /** Nombre de la plantilla Thymeleaf asociada (sin extension). */
    public String getTemplateName() {
        return templateName;
    }
}
