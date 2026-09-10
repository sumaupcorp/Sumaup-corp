package com.sumaup360.erp.documenttemplate.service;

import com.sumaup360.erp.documenttemplate.dto.DocumentData;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Datos de EJEMPLO universales para la vista previa. Sirven a todos los rubros; los campos
 * por rubro se agregan segun businessType. Los importes son ilustrativos.
 */
@Component
public class DocumentSampleDataFactory {

    // TODO SUNAT: la tasa de IGV y el calculo definitivo deben centralizarse en lo tributario.
    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    public DocumentData build(DocumentType documentType, String businessType) {
        List<DocumentData.Item> items = sampleItems(businessType);

        BigDecimal subtotal = items.stream().map(DocumentData.Item::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.ONE.add(IGV_RATE), 2, RoundingMode.HALF_UP);
        BigDecimal total = items.stream().map(DocumentData.Item::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal igv = total.subtract(subtotal);

        DocumentData.Totals totals = DocumentData.Totals.builder()
                .subtotal(subtotal)
                .discountTotal(BigDecimal.ZERO)
                .igv(igv)
                .total(total)
                .amountInWords("SON: " + total.toPlainString() + " SOLES")  // TODO: numero a letras
                .build();

        DocumentData data = DocumentData.builder()
                .company(DocumentData.Company.builder()
                        .razonSocial("EMPRESA DEMO SUMAUP S.A.C.")
                        .nombreComercial("Negocio Demo")
                        .ruc("20123456789")
                        .direccionFiscal("Av. Principal 123, Lima")
                        .direccionSucursal("Sucursal Centro - Jr. Comercio 456")
                        .telefono("(01) 555-1234")
                        .email("ventas@demo.pe")
                        .web("www.demo.pe")
                        .logoUrl(null)
                        .build())
                .customer(DocumentData.Customer.builder()
                        .docType(documentType == DocumentType.INVOICE ? "RUC" : "DNI")
                        .docNumber(documentType == DocumentType.INVOICE ? "20498765432" : "45678912")
                        .name(documentType == DocumentType.INVOICE
                                ? "CLIENTE EMPRESA S.A.C." : "Juan Perez Quispe")
                        .address("Av. Cliente 789, Lima")
                        .build())
                .meta(DocumentData.Meta.builder()
                        .documentTypeLabel(documentType.getLabel())
                        .series(defaultSeries(documentType))
                        .number("000123")
                        .fullNumber(defaultSeries(documentType) + "-000123")
                        .issueDate("2026-06-28 13:45")
                        .currencyCode("PEN")
                        .currencySymbol("S/")
                        .paymentMethod("Efectivo")
                        .cashier("Cajero Demo")
                        .observations("Gracias por su compra.")
                        .build())
                .items(items)
                .totals(totals)
                .build();

        applyBusinessBlock(data, businessType);
        return data;
    }

    private List<DocumentData.Item> sampleItems(String businessType) {
        DocumentData.Item i1 = DocumentData.Item.builder()
                .code("P001").description("Producto / servicio de ejemplo 1").unit("UND")
                .quantity(new BigDecimal("2")).unitPrice(new BigDecimal("15.00"))
                .discount(BigDecimal.ZERO).igv(new BigDecimal("4.58")).total(new BigDecimal("30.00"))
                .build();
        DocumentData.Item i2 = DocumentData.Item.builder()
                .code("P002").description("Producto / servicio de ejemplo 2").unit("UND")
                .quantity(new BigDecimal("1")).unitPrice(new BigDecimal("25.00"))
                .discount(BigDecimal.ZERO).igv(new BigDecimal("3.81")).total(new BigDecimal("25.00"))
                .build();
        if ("hardware".equalsIgnoreCase(businessType)) {
            i1.setSku("FER-001"); i1.setBrand("Stanley");
            i2.setSku("FER-002"); i2.setBrand("Truper");
        }
        if ("pharmacy".equalsIgnoreCase(businessType)) {
            i1.setBatch("L2026A"); i1.setExpiryDate("2027-05-31");
            i2.setBatch("L2026B"); i2.setExpiryDate("2026-12-31");
        }
        return List.of(i1, i2);
    }

    private void applyBusinessBlock(DocumentData data, String businessType) {
        if (businessType == null) {
            return;
        }
        switch (businessType.toLowerCase()) {
            case "restaurant" -> data.setRestaurant(DocumentData.Restaurant.builder()
                    .table("Mesa 5").waiter("Mozo Carlos").room("Salon principal")
                    .order("CMD-018").attentionTime("00:35").build());
            case "hardware" -> data.setHardware(DocumentData.Hardware.builder()
                    .warehouse("Almacen Central").build());
            case "pharmacy" -> data.setPharmacy(DocumentData.Pharmacy.builder()
                    .saleResponsible("Q.F. Maria Lopez").build());
            case "delivery" -> data.setDelivery(DocumentData.Delivery.builder()
                    .deliveryAddress("Av. Entrega 321, Surco").courier("Repartidor Luis")
                    .deliveryMethod("Moto").build());
            default -> { /* sin bloque por rubro */ }
        }
    }

    private String defaultSeries(DocumentType type) {
        return switch (type) {
            case INVOICE, DEBIT_NOTE, CREDIT_NOTE -> "F001";
            case SALE_RECEIPT -> "B001";
            case DELIVERY_GUIDE -> "T001";
            default -> "NV01";
        };
    }
}
