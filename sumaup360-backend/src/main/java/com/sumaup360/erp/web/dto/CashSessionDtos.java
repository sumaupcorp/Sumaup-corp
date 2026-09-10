package com.sumaup360.erp.web.dto;

import com.sumaup360.erp.domain.CashMovement;
import com.sumaup360.erp.domain.CashSession;
import com.sumaup360.erp.service.CashSessionService.CashSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CashSessionDtos {

    private CashSessionDtos() {
    }

    public record OpenCashRequest(
            @NotNull UUID branchId,
            @PositiveOrZero BigDecimal openingAmount
    ) {
    }

    /** Cierre con arqueo: efectivo contado fisicamente + notas opcionales. */
    public record CloseCashRequest(
            @NotNull @PositiveOrZero BigDecimal countedAmount,
            @Size(max = 300) String notes
    ) {
    }

    /** Ingreso o salida de efectivo de la caja (gastos, retiros, sencillo, cobranzas). */
    public record CashMovementRequest(
            @NotBlank String type,
            @NotBlank String category,
            @NotBlank @Size(max = 200) String concept,
            @NotNull @Positive BigDecimal amount
    ) {
    }

    public record CashMovementResponse(UUID id, String type, String category, String concept,
                                       BigDecimal amount, OffsetDateTime createdAt) {
        public static CashMovementResponse from(CashMovement m) {
            return new CashMovementResponse(m.getId(), m.getType(), m.getCategory(),
                    m.getConcept(), m.getAmount(), m.getCreatedAt());
        }
    }

    public record CashSessionResponse(UUID id, UUID branchId, String status,
                                      BigDecimal openingAmount, BigDecimal closingAmount,
                                      BigDecimal expectedAmount, BigDecimal difference,
                                      String notes,
                                      OffsetDateTime openedAt, OffsetDateTime closedAt) {
        public static CashSessionResponse from(CashSession s) {
            return new CashSessionResponse(s.getId(), s.getBranchId(), s.getStatus(),
                    s.getOpeningAmount(), s.getClosingAmount(), s.getExpectedAmount(),
                    s.getDifference(), s.getNotes(), s.getOpenedAt(), s.getClosedAt());
        }
    }

    /** Cierre de caja (arqueo) del historial: cuadre y diferencia de cada sesion. */
    public record CashClosureResponse(
            UUID id,
            UUID branchId,
            String branchName,
            OffsetDateTime openedAt,
            OffsetDateTime closedAt,
            BigDecimal openingAmount,
            BigDecimal expectedAmount,
            BigDecimal closingAmount,
            BigDecimal difference,
            String notes
    ) {
        public static CashClosureResponse from(CashSession s, String branchName) {
            return new CashClosureResponse(s.getId(), s.getBranchId(), branchName,
                    s.getOpenedAt(), s.getClosedAt(), s.getOpeningAmount(),
                    s.getExpectedAmount(), s.getClosingAmount(), s.getDifference(), s.getNotes());
        }
    }

    /** Resumen vivo de la caja para el panel y el modal de arqueo. */
    public record CashSummaryResponse(
            UUID sessionId,
            String status,
            OffsetDateTime openedAt,
            String openedByName,
            BigDecimal openingAmount,
            BigDecimal salesTotal,
            long salesCount,
            BigDecimal cashSales,
            Map<String, BigDecimal> salesByMethod,
            BigDecimal incomesTotal,
            BigDecimal expensesTotal,
            BigDecimal expectedCash,
            BigDecimal closingAmount,
            BigDecimal difference,
            List<CashMovementResponse> movements
    ) {
        public static CashSummaryResponse from(CashSummary s) {
            return new CashSummaryResponse(
                    s.session().getId(), s.session().getStatus(), s.session().getOpenedAt(),
                    s.openedByName(), s.session().getOpeningAmount(),
                    s.salesTotal(), s.salesCount(), s.cashSales(), s.salesByMethod(),
                    s.incomesTotal(), s.expensesTotal(), s.expectedCash(),
                    s.session().getClosingAmount(), s.session().getDifference(),
                    s.movements().stream().map(CashMovementResponse::from).toList());
        }
    }
}
