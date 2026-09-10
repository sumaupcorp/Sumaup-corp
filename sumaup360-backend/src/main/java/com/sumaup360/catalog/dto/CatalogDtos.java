package com.sumaup360.catalog.dto;

/** Vistas de salida de los catalogos. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CodeName(String code, String name) {
    }

    public record CurrencyView(String code, String name, String symbol) {
    }

    public record ModuleView(String code, String name, boolean core) {
    }

    public record VerticalView(String code, String name, String businessTypeCode) {
    }
}
