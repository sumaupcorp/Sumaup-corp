package com.sumaup360.erp.documenttemplate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Datos universales de un documento para renderizar (sirve a TODOS los rubros).
 * Los bloques por rubro (restaurant, hardware, pharmacy, delivery) son OPCIONALES.
 * Se usa tanto como payload de entrada (Jackson) como modelo para Thymeleaf (getters).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentData {

    private Company company;
    private Customer customer;
    private Meta meta;
    @Builder.Default
    private List<Item> items = List.of();
    private Totals totals;

    // Bloques opcionales por rubro (se muestran solo si vienen presentes).
    private Restaurant restaurant;
    private Hardware hardware;
    private Pharmacy pharmacy;
    private Delivery delivery;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Company {
        private String razonSocial;
        private String nombreComercial;
        private String ruc;
        private String direccionFiscal;
        private String direccionSucursal;
        private String telefono;
        private String email;
        private String web;
        private String logoUrl;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Customer {
        private String docType;
        private String docNumber;
        private String name;
        private String address;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Meta {
        private String documentTypeLabel;
        private String series;
        private String number;
        private String fullNumber;
        private String issueDate;
        private String currencyCode;
        private String currencySymbol;
        private String paymentMethod;
        private String cashier;
        private String observations;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Item {
        private String code;
        private String description;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal discount;
        private BigDecimal igv;
        private BigDecimal total;
        // Extras opcionales por rubro (mostrados solo si la plantilla lo permite).
        private String sku;
        private String brand;
        private String batch;
        private String expiryDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Totals {
        private BigDecimal subtotal;
        private BigDecimal discountTotal;
        private BigDecimal igv;
        private BigDecimal total;
        private String amountInWords;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Restaurant {
        private String table;
        private String waiter;
        private String room;
        private String order;
        private String attentionTime;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Hardware {
        private String warehouse;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Pharmacy {
        private String saleResponsible;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Delivery {
        private String deliveryAddress;
        private String courier;
        private String deliveryMethod;
    }
}
