package com.sumaup360.backoffice.web;

import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.dto.BillingDtos.ChangeStatusRequest;
import com.sumaup360.billing.dto.BillingDtos.SubscribeTenantRequest;
import com.sumaup360.billing.dto.BillingDtos.SubscriptionView;
import com.sumaup360.billing.service.SubscriptionService;
import com.sumaup360.backoffice.service.ClientHistoryService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Backoffice (staff): gestion de licencias (suscripciones) de los tenants. Cross-tenant. */
@RestController
@RequestMapping("/api/v1/backoffice/licenses")
@Tag(name = "Backoffice - Licencias", description = "Suscripciones/licencias del SaaS por tenant")
public class BackofficeLicenseController {

    private final SubscriptionService subscriptionService;
    private final ClientHistoryService history;

    public BackofficeLicenseController(SubscriptionService subscriptionService,
                                       ClientHistoryService history) {
        this.subscriptionService = subscriptionService;
        this.history = history;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('license:read')")
    @Operation(summary = "Lista las licencias de un tenant (requiere license:read)")
    public List<SubscriptionView> list(@RequestParam UUID tenantId) {
        return subscriptionService.listByTenant(tenantId).stream()
                .map(s -> SubscriptionView.from(s, subscriptionService.planCodeOf(s.getPlanId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('license:manage')")
    @Operation(summary = "Asigna una licencia (plan BUSINESS) a un tenant (requiere license:manage)")
    public SubscriptionView assign(@Valid @RequestBody SubscribeTenantRequest req) {
        Subscription s = subscriptionService.subscribeTenant(req.tenantId(), req.planCode());
        history.record(req.tenantId(), "PLAN_ASSIGNED", "Plan asignado: " + req.planCode(),
                SecurityUtils.currentPrincipal().userId());
        return SubscriptionView.from(s, subscriptionService.planCodeOf(s.getPlanId()));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('license:manage')")
    @Operation(summary = "Cambia el estado de una licencia (requiere license:manage)")
    public SubscriptionView changeStatus(@PathVariable UUID id,
                                         @Valid @RequestBody ChangeStatusRequest req) {
        Subscription s = subscriptionService.changeStatus(id, req.status());
        history.record(s.getTenantId(), "LICENSE_STATUS", "Licencia cambiada a " + req.status(),
                SecurityUtils.currentPrincipal().userId());
        return SubscriptionView.from(s, subscriptionService.planCodeOf(s.getPlanId()));
    }
}
