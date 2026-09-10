package com.sumaup360.erp.batch.dto;

import com.sumaup360.erp.batch.domain.ProductBatch;
import com.sumaup360.erp.batch.enums.BatchStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class BatchDtos {

    private BatchDtos() {
    }

    public record CreateBatchRequest(
            @NotNull UUID branchId,
            @NotNull UUID productId,
            @NotBlank @Size(max = 60) String batchCode,
            LocalDate expiryDate,
            @NotNull @PositiveOrZero BigDecimal quantity,
            @Size(max = 300) String notes
    ) {
    }

    public record UpdateBatchRequest(
            @PositiveOrZero BigDecimal quantity,
            LocalDate expiryDate,
            BatchStatus status,
            @Size(max = 300) String notes
    ) {
    }

    public record BatchResponse(UUID id, UUID branchId, UUID productId, String productName,
                                String batchCode, LocalDate expiryDate, BigDecimal quantity,
                                BatchStatus status, String notes) {
        public static BatchResponse from(ProductBatch b, String productName) {
            return new BatchResponse(b.getId(), b.getBranchId(), b.getProductId(), productName,
                    b.getBatchCode(), b.getExpiryDate(), b.getQuantity(), b.getStatus(), b.getNotes());
        }
    }
}
