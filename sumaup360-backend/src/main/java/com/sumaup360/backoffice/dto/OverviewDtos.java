package com.sumaup360.backoffice.dto;

import java.util.List;

public final class OverviewDtos {

    private OverviewDtos() {
    }

    public record PlanCount(String plan, long count) {
    }

    /** Resumen agregado para el dashboard del Backoffice. */
    public record Overview(
            long clients,
            long companies,
            long branches,
            long users,
            long staff,
            long pendingReceipts,
            long inProcessReceipts,
            long processedReceipts,
            long activeSubscriptions,
            List<PlanCount> byPlan
    ) {
    }
}
