package com.sumaup360.erp.documenttemplate.enums;

/**
 * Tipos de documento soportados.
 *
 * Comerciales/tributarios (preparados para facturacion electronica futura):
 * SALE_RECEIPT (boleta), INVOICE (factura), CREDIT_NOTE, DEBIT_NOTE, DELIVERY_GUIDE.
 * Internos (NO son comprobantes tributarios): INTERNAL_SALE_NOTE, POS_TICKET, KITCHEN_ORDER
 * (comanda), PRE_BILL (precuenta), QUOTATION (cotizacion).
 */
public enum DocumentType {
    SALE_RECEIPT(true, "BOLETA DE VENTA"),
    INVOICE(true, "FACTURA"),
    INTERNAL_SALE_NOTE(false, "NOTA DE VENTA"),
    DELIVERY_GUIDE(true, "GUIA DE REMISION"),
    POS_TICKET(false, "TICKET"),
    KITCHEN_ORDER(false, "COMANDA"),
    PRE_BILL(false, "PRECUENTA"),
    QUOTATION(false, "COTIZACION"),
    CREDIT_NOTE(true, "NOTA DE CREDITO"),
    DEBIT_NOTE(true, "NOTA DE DEBITO");

    /** true si el documento es tributario y debera integrarse con SUNAT a futuro. */
    private final boolean fiscal;
    private final String label;

    DocumentType(boolean fiscal, String label) {
        this.fiscal = fiscal;
        this.label = label;
    }

    public boolean isFiscal() {
        return fiscal;
    }

    public String getLabel() {
        return label;
    }
}
