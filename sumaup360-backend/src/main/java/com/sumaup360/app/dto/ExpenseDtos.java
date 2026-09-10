package com.sumaup360.app.dto;

import com.sumaup360.app.domain.Expense;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ExpenseDtos {

    private ExpenseDtos() {
    }

    public record CreateExpenseRequest(
            @NotNull LocalDate txDate,
            @NotNull @Positive BigDecimal amount,
            String currency,
            String category,
            String paymentMethod,
            String description,
            UUID receiptId
    ) {
    }

    public record ExpenseResponse(UUID id, LocalDate txDate, BigDecimal amount, String currency,
                                  String category, String paymentMethod, String description,
                                  UUID receiptId) {
        public static ExpenseResponse from(Expense e) {
            return new ExpenseResponse(e.getId(), e.getTxDate(), e.getAmount(), e.getCurrency(),
                    e.getCategory(), e.getPaymentMethod(), e.getDescription(), e.getReceiptId());
        }
    }
}
