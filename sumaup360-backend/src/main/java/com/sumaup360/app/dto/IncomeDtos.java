package com.sumaup360.app.dto;

import com.sumaup360.app.domain.Income;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class IncomeDtos {

    private IncomeDtos() {
    }

    public record CreateIncomeRequest(
            @NotNull LocalDate txDate,
            @NotNull @Positive BigDecimal amount,
            String currency,
            String category,
            String paymentMethod,
            String description,
            UUID receiptId
    ) {
    }

    public record IncomeResponse(UUID id, LocalDate txDate, BigDecimal amount, String currency,
                                 String category, String paymentMethod, String description,
                                 UUID receiptId) {
        public static IncomeResponse from(Income i) {
            return new IncomeResponse(i.getId(), i.getTxDate(), i.getAmount(), i.getCurrency(),
                    i.getCategory(), i.getPaymentMethod(), i.getDescription(), i.getReceiptId());
        }
    }
}
