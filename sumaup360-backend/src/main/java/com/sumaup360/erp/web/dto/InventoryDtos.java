package com.sumaup360.erp.web.dto;

import com.sumaup360.erp.domain.InventoryMovement;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    public record AdjustStockRequest(
            @NotNull UUID productId,
            @NotNull UUID branchId,
            @NotNull BigDecimal quantity,   // delta con signo (+ ingresa, - retira)
            String reason
    ) {
    }

    public record StockResponse(UUID productId, UUID branchId, BigDecimal quantity) {
    }

    /** Un dia del flujo semanal: unidades que entraron y salieron. */
    public record DailyFlowResponse(java.time.LocalDate date, BigDecimal inQty, BigDecimal outQty) {
    }

    /** Movimiento del kardex: IN/OUT/ADJUST con motivo y fecha. */
    public record MovementResponse(UUID id, UUID productId, String type,
                                   BigDecimal quantity, String reason, OffsetDateTime createdAt) {
        public static MovementResponse from(InventoryMovement m) {
            return new MovementResponse(m.getId(), m.getProductId(), m.getType(),
                    m.getQuantity(), m.getReason(), m.getCreatedAt());
        }
    }
}
