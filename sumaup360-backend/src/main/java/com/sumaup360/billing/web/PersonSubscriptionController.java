package com.sumaup360.billing.web;

import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.dto.BillingDtos.SubscribeUserRequest;
import com.sumaup360.billing.dto.BillingDtos.SubscriptionView;
import com.sumaup360.billing.service.SubscriptionService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Suscripcion de la persona autenticada (Linea Personas). */
@RestController
@RequestMapping("/api/v1/app/subscription")
@Tag(name = "Personas - Suscripcion", description = "Plan de la persona")
public class PersonSubscriptionController {

    private final SubscriptionService subscriptionService;

    public PersonSubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    @Operation(summary = "Lista las suscripciones de la persona")
    public List<SubscriptionView> mine() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return subscriptionService.listByUser(userId).stream()
                .map(s -> SubscriptionView.from(s, subscriptionService.planCodeOf(s.getPlanId())))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Suscribe a la persona a un plan de la linea Personas")
    public SubscriptionView subscribe(@Valid @RequestBody SubscribeUserRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        Subscription s = subscriptionService.subscribeUser(userId, req.planCode());
        return SubscriptionView.from(s, subscriptionService.planCodeOf(s.getPlanId()));
    }
}
