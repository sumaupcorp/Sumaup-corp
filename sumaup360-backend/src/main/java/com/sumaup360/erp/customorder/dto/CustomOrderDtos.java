package com.sumaup360.erp.customorder.dto;

import com.sumaup360.erp.customorder.domain.CustomOrder;
import com.sumaup360.erp.customorder.enums.CustomOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class CustomOrderDtos {

    private CustomOrderDtos() {
    }

    public record CreateCustomOrderRequest(
            @NotNull UUID branchId,
            @NotNull UUID customerId,
            @NotBlank @Size(max = 500) String description,
            @NotNull OffsetDateTime deliveryAt,
            @NotNull @PositiveOrZero BigDecimal totalAmount,
            @PositiveOrZero BigDecimal advanceAmount,
            @Size(max = 300) String notes
    ) {
    }

    public record UpdateCustomOrderRequest(
            @Size(max = 500) String description,
            OffsetDateTime deliveryAt,
            @PositiveOrZero BigDecimal totalAmount,
            @PositiveOrZero BigDecimal advanceAmount,
            @Size(max = 300) String notes
    ) {
    }

    public record UpdateCustomOrderStatusRequest(@NotNull CustomOrderStatus status) {
    }

    public record CustomOrderResponse(UUID id, UUID branchId, UUID customerId, String customerName,
                                      String description, OffsetDateTime deliveryAt,
                                      BigDecimal totalAmount, BigDecimal advanceAmount,
                                      CustomOrderStatus status, UUID saleId, String notes) {
        public static CustomOrderResponse from(CustomOrder o, String customerName) {
            return new CustomOrderResponse(o.getId(), o.getBranchId(), o.getCustomerId(), customerName,
                    o.getDescription(), o.getDeliveryAt(), o.getTotalAmount(), o.getAdvanceAmount(),
                    o.getStatus(), o.getSaleId(), o.getNotes());
        }
    }
}
