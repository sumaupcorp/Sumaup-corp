package com.sumaup360.app.dto;

import java.math.BigDecimal;

public final class SummaryDtos {

    private SummaryDtos() {
    }

    /** Resumen del mes para el home de la app. */
    public record SummaryResponse(String period, BigDecimal totalIncome,
                                  BigDecimal totalExpense, BigDecimal utility, String currency) {
    }
}
