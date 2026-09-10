package com.sumaup360.billing.service;

import com.sumaup360.billing.enums.SubscriptionStatus;
import com.sumaup360.billing.repository.PlanRepository;
import com.sumaup360.billing.repository.SubscriptionRepository;
import com.sumaup360.tenant.PlanLimitProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Resuelve los limites desde la suscripcion ACTIVE del tenant y su plan. */
@Component
public class BillingPlanLimitProvider implements PlanLimitProvider {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public BillingPlanLimitProvider(SubscriptionRepository subscriptionRepository,
                                    PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PlanLimits limits(UUID tenantId) {
        return subscriptionRepository.findFirstByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)
                .flatMap(s -> planRepository.findById(s.getPlanId()))
                .map(p -> new PlanLimits(p.getMaxBranches(), p.getMaxUsers()))
                .orElse(PlanLimits.unlimited());
    }
}
