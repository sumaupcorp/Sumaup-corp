package com.sumaup360.erp.web.dto;

import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.domain.SaleItem;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SaleDtos {

    private SaleDtos() {
    }

    public record SaleItemRequest(
            @NotNull UUID productId,
            @NotNull BigDecimal quantity
    ) {
    }

    public record CreateSaleRequest(
            @NotNull UUID branchId,
            UUID customerId,
            /** CASH (defecto), CARD, YAPE, PLIN, TRANSFER. */
            String paymentMethod,
            @NotEmpty List<SaleItemRequest> items
    ) {
    }

    /** Un dia del flujo semanal de ventas: total, efectivo y digital. */
    public record DailySalesResponse(java.time.LocalDate date, BigDecimal total,
                                     BigDecimal cashTotal, BigDecimal digitalTotal, long count) {
    }

    public record SaleItemResponse(UUID productId, String description, BigDecimal quantity,
                                   BigDecimal unitPrice, BigDecimal lineTotal) {
        public static SaleItemResponse from(SaleItem i) {
            return new SaleItemResponse(i.getProductId(), i.getDescription(), i.getQuantity(),
                    i.getUnitPrice(), i.getLineTotal());
        }
    }

    public record SaleResponse(UUID id, UUID branchId, UUID cashSessionId, UUID customerId,
                               BigDecimal total, String status, String paymentMethod,
                               OffsetDateTime createdAt, List<SaleItemResponse> items) {
        public static SaleResponse from(Sale s, List<SaleItem> items) {
            return new SaleResponse(s.getId(), s.getBranchId(), s.getCashSessionId(),
                    s.getCustomerId(), s.getTotal(), s.getStatus(), s.getPaymentMethod(),
                    s.getCreatedAt(), items.stream().map(SaleItemResponse::from).toList());
        }
    }
}
