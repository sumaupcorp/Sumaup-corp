package com.sumaup360.app.dto;

import com.sumaup360.app.domain.Receipt;
import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.enums.ReceiptType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class ReceiptDtos {

    private ReceiptDtos() {
    }

    public record CreateReceiptRequest(
            ReceiptType type,
            String docNumber,
            LocalDate issueDate,
            BigDecimal amount,
            String currency,
            String fileUrl,
            String notes
    ) {
    }

    public record ReceiptResponse(UUID id, ReceiptType type, String docNumber, LocalDate issueDate,
                                  BigDecimal amount, String currency, String fileUrl,
                                  ReceiptStatus status, String notes) {
        public static ReceiptResponse from(Receipt r) {
            return new ReceiptResponse(r.getId(), r.getType(), r.getDocNumber(), r.getIssueDate(),
                    r.getAmount(), r.getCurrency(), r.getFileUrl(), r.getStatus(), r.getNotes());
        }
    }
}
