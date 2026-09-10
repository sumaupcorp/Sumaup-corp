package com.sumaup360.billing.service;

import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.enums.SubscriptionStatus;
import com.sumaup360.billing.repository.PlanRepository;
import com.sumaup360.billing.repository.SubscriptionRepository;
import com.sumaup360.erp.module.LicenseModuleProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Resuelve el techo de modulos desde la suscripcion ACTIVE del tenant y su plan. */
@Component
public class BillingLicenseModuleProvider implements LicenseModuleProvider {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public BillingLicenseModuleProvider(SubscriptionRepository subscriptionRepository,
                                        PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Set<String>> allowedModules(UUID tenantId) {
        Subscription sub = subscriptionRepository
                .findFirstByTenantIdAndStatus(tenantId, SubscriptionStatus.ACTIVE)
                .orElse(null);
        if (sub == null) {
            return Optional.empty(); // sin licencia activa => sin techo
        }
        return planRepository.findById(sub.getPlanId())
                .map(plan -> (Set<String>) new HashSet<>(plan.getModuleCodes()));
    }
}
