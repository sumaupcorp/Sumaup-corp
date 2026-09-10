package com.sumaup360.billing.domain;

import com.sumaup360.billing.enums.SubscriberType;
import com.sumaup360.billing.enums.SubscriptionStatus;
import com.sumaup360.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/** Suscripcion/membresia: vincula un tenant (licencia SaaS) o un usuario (persona) a un plan. */
@Entity
@Table(name = "subscription", schema = "billing")
@Getter
@Setter
public class Subscription extends BaseEntity {

    @Column(name = "plan_id", nullable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriber_type", nullable = false, length = 20)
    private SubscriberType subscriberType;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
