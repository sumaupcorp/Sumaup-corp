package com.sumaup360.billing.dto;

import com.sumaup360.billing.domain.Plan;
import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.enums.ProductLine;
import com.sumaup360.billing.enums.SubscriberType;
import com.sumaup360.billing.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class BillingDtos {

    private BillingDtos() {
    }

    public record PlanView(String code, String name, ProductLine line, BigDecimal price,
                           String currency, Integer maxBranches, Integer maxUsers,
                           Set<String> moduleCodes) {
        public static PlanView from(Plan p) {
            // Copia la coleccion (lazy) a un Set propio dentro de la transaccion.
            return new PlanView(p.getCode(), p.getName(), p.getLine(), p.getPrice(),
                    p.getCurrency(), p.getMaxBranches(), p.getMaxUsers(),
                    new HashSet<>(p.getModuleCodes()));
        }
    }

    public record CreatePlanRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull ProductLine line,
            BigDecimal price,
            Integer maxBranches,
            Integer maxUsers,
            List<String> moduleCodes
    ) {
    }

    /** Suscribir una empresa a un plan (licencia SaaS) — uso Backoffice. */
    public record SubscribeTenantRequest(@NotNull UUID tenantId, @NotBlank String planCode) {
    }

    /** Suscribir a la persona autenticada a un plan. */
    public record SubscribeUserRequest(@NotBlank String planCode) {
    }

    public record ChangeStatusRequest(@NotNull SubscriptionStatus status) {
    }

    public record SubscriptionView(UUID id, String planCode, SubscriberType subscriberType,
                                   UUID tenantId, UUID userId, SubscriptionStatus status,
                                   LocalDate startDate, LocalDate endDate) {
        public static SubscriptionView from(Subscription s, String planCode) {
            return new SubscriptionView(s.getId(), planCode, s.getSubscriberType(),
                    s.getTenantId(), s.getUserId(), s.getStatus(), s.getStartDate(), s.getEndDate());
        }
    }
}
