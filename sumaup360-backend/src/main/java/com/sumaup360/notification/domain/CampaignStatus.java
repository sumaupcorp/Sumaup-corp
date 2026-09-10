package com.sumaup360.notification.domain;

/** Ciclo de vida de una campana de push. */
public enum CampaignStatus {
    SCHEDULED,
    SENDING,
    SENT,
    CANCELLED
}
