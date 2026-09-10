package com.sumaup360.erp.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** DTOs de salida de los reportes ERP. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record SalesSummaryResponse(OffsetDateTime from, OffsetDateTime to, UUID branchId,
                                       long salesCount, BigDecimal totalAmount) {
    }

    public record TopProductResponse(UUID productId, String sku, String name,
                                     BigDecimal quantitySold, BigDecimal amount) {
    }

    public record LowStockResponse(UUID productId, String sku, String name,
                                   UUID branchId, BigDecimal quantity) {
    }

    public record SalesByBranchResponse(UUID branchId, String branchName,
                                        long salesCount, BigDecimal totalAmount) {
    }

    /** Ventas por metodo de pago en el rango. */
    public record PaymentMethodStatResponse(String method, long count, BigDecimal total) {
    }
}
