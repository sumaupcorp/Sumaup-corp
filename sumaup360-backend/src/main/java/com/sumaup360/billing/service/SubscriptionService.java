package com.sumaup360.billing.service;

import com.sumaup360.billing.domain.Plan;
import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.enums.ProductLine;
import com.sumaup360.billing.enums.SubscriberType;
import com.sumaup360.billing.enums.SubscriptionStatus;
import com.sumaup360.billing.repository.PlanRepository;
import com.sumaup360.billing.repository.SubscriptionRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Suscripciones: licencias de tenant (Backoffice) y planes de persona (app). */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final TenantRepository tenantRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PlanRepository planRepository,
                               TenantRepository tenantRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public Subscription subscribeTenant(UUID tenantId, String planCode) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado."));
        Plan plan = requirePlan(planCode);
        if (plan.getLine() != ProductLine.BUSINESS) {
            throw new BadRequestException("El plan no es de la linea Negocios.");
        }
        cancelActive(subscriptionRepository.findByTenantId(tenantId));
        Subscription s = new Subscription();
        s.setPlanId(plan.getId());
        s.setSubscriberType(SubscriberType.TENANT);
        s.setTenantId(tenantId);
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setStartDate(LocalDate.now());
        return subscriptionRepository.save(s);
    }

    @Transactional
    public Subscription subscribeUser(UUID userId, String planCode) {
        Plan plan = requirePlan(planCode);
        if (plan.getLine() != ProductLine.PERSON) {
            throw new BadRequestException("El plan no es de la linea Personas.");
        }
        cancelActive(subscriptionRepository.findByUserId(userId));
        Subscription s = new Subscription();
        s.setPlanId(plan.getId());
        s.setSubscriberType(SubscriberType.USER);
        s.setUserId(userId);
        s.setStatus(SubscriptionStatus.ACTIVE);
        s.setStartDate(LocalDate.now());
        return subscriptionRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<Subscription> listByTenant(UUID tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> listByUser(UUID userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    @Transactional
    public Subscription changeStatus(UUID id, SubscriptionStatus status) {
        Subscription s = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripcion no encontrada."));
        s.setStatus(status);
        if (status == SubscriptionStatus.CANCELLED && s.getEndDate() == null) {
            s.setEndDate(LocalDate.now());
        }
        return subscriptionRepository.save(s);
    }

    public String planCodeOf(UUID planId) {
        return planRepository.findById(planId).map(Plan::getCode).orElse(null);
    }

    /** Plan activo (codigo) del usuario, o null. */
    @Transactional(readOnly = true)
    public String activePlanCode(UUID userId) {
        return subscriptionRepository.findFirstByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(s -> planCodeOf(s.getPlanId()))
                .orElse(null);
    }

    /** True si el usuario tiene una suscripcion premium activa. */
    @Transactional(readOnly = true)
    public boolean isUserPremium(UUID userId) {
        String code = activePlanCode(userId);
        return code != null && code.toLowerCase().contains("premium");
    }

    /** Activa premium manualmente (backoffice). Crea suscripcion ACTIVE con vigencia en meses. */
    @Transactional
    public Subscription activatePremium(UUID userId, String planCode, int months) {
        Subscription s = subscribeUser(userId, planCode); // valida PERSON + cancela activas
        s.setEndDate(LocalDate.now().plusMonths(months <= 0 ? 1 : months));
        return subscriptionRepository.save(s);
    }

    /** Cancela la suscripcion activa del usuario (vuelve a FREE). */
    @Transactional
    public void cancelUserActive(UUID userId) {
        subscriptionRepository.findFirstByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(s -> changeStatus(s.getId(), SubscriptionStatus.CANCELLED));
    }

    private void cancelActive(List<Subscription> subs) {
        subs.stream().filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE).forEach(s -> {
            s.setStatus(SubscriptionStatus.CANCELLED);
            s.setEndDate(LocalDate.now());
            subscriptionRepository.save(s);
        });
    }

    private Plan requirePlan(String planCode) {
        return planRepository.findByCode(planCode)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado: " + planCode));
    }
}
