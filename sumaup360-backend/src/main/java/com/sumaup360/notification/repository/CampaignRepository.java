package com.sumaup360.notification.repository;

import com.sumaup360.notification.domain.Campaign;
import com.sumaup360.notification.domain.CampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    List<Campaign> findAllByOrderByCreatedAtDesc();

    /** Campanas programadas cuya hora ya paso (para el poller). */
    List<Campaign> findByStatusAndScheduledAtLessThanEqual(CampaignStatus status, OffsetDateTime limit);

    /**
     * Reclama la campana para envio de forma atomica (SCHEDULED -> SENDING).
     * Devuelve 1 si este hilo la gano; 0 si otro proceso ya la tomo o fue cancelada.
     */
    @Modifying
    @Transactional
    @Query("update Campaign c set c.status = :sending, c.updatedAt = :now "
            + "where c.id = :id and c.status = :scheduled")
    int claimForSending(@Param("id") UUID id,
                        @Param("sending") CampaignStatus sending,
                        @Param("scheduled") CampaignStatus scheduled,
                        @Param("now") OffsetDateTime now);
}
