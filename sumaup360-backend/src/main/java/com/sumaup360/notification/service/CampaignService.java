package com.sumaup360.notification.service;

import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.domain.Campaign;
import com.sumaup360.notification.domain.CampaignAudience;
import com.sumaup360.notification.domain.CampaignStatus;
import com.sumaup360.notification.repository.CampaignRepository;
import com.sumaup360.notification.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Campanas de push del Backoffice: creacion (inmediata o programada), cancelacion y
 * envio a la audiencia elegida. El poller reclama las campanas programadas de forma
 * atomica (SCHEDULED -> SENDING) para proteger contra el doble envio.
 */
@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final PersonProfileRepository profileRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushService pushService;

    public CampaignService(CampaignRepository campaignRepository,
                           PersonProfileRepository profileRepository,
                           DeviceTokenRepository deviceTokenRepository,
                           PushService pushService) {
        this.campaignRepository = campaignRepository;
        this.profileRepository = profileRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushService = pushService;
    }

    @Transactional(readOnly = true)
    public List<Campaign> list() {
        return campaignRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Crea una campana. Si no tiene fecha programada (o ya paso) se envia de inmediato
     * y queda SENT; si la fecha es futura queda SCHEDULED y la toma el poller.
     */
    @Transactional
    public Campaign create(String title, String body, String route, String audience,
                           OffsetDateTime scheduledAt, UUID staffUserId) {
        if (title == null || title.isBlank()) {
            throw new BadRequestException("El titulo de la campana es obligatorio.");
        }
        if (body == null || body.isBlank()) {
            throw new BadRequestException("El mensaje de la campana es obligatorio.");
        }
        CampaignAudience aud = parseAudience(audience);

        Campaign c = new Campaign();
        c.setTitle(title.trim());
        c.setBody(body.trim());
        c.setRoute(route != null && !route.isBlank() ? route.trim() : null);
        c.setAudience(aud);
        c.setScheduledAt(scheduledAt);
        c.setCreatedBy(staffUserId);

        boolean immediate = scheduledAt == null || !scheduledAt.isAfter(OffsetDateTime.now());
        if (immediate) {
            c.setStatus(CampaignStatus.SENDING);
            c = campaignRepository.save(c);
            deliver(c);
        } else {
            c.setStatus(CampaignStatus.SCHEDULED);
            c = campaignRepository.save(c);
        }
        return c;
    }

    /** Cancela una campana programada. Solo es cancelable mientras esta SCHEDULED. */
    @Transactional
    public Campaign cancel(UUID id) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campana no encontrada."));
        if (c.getStatus() != CampaignStatus.SCHEDULED) {
            throw new ConflictException("Solo se puede cancelar una campana programada (SCHEDULED).");
        }
        c.setStatus(CampaignStatus.CANCELLED);
        return campaignRepository.save(c);
    }

    /**
     * Poller de campanas programadas: cada minuto busca SCHEDULED vencidas, las reclama
     * de forma atomica y las envia. Si otro proceso ya reclamo la campana, la salta.
     */
    @Scheduled(fixedDelay = 60_000)
    public void pollScheduled() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Campaign> due = campaignRepository
                .findByStatusAndScheduledAtLessThanEqual(CampaignStatus.SCHEDULED, now);
        for (Campaign c : due) {
            int claimed = campaignRepository.claimForSending(
                    c.getId(), CampaignStatus.SENDING, CampaignStatus.SCHEDULED, now);
            if (claimed == 0) {
                continue; // otro proceso la tomo, o fue cancelada entre la consulta y el claim
            }
            c.setStatus(CampaignStatus.SENDING);
            try {
                deliver(c);
            } catch (Exception e) {
                // El envio es fail-safe por usuario; esto cubre fallos inesperados del lote.
                log.error("Error enviando la campana programada {}: {}", c.getId(), e.getMessage());
            }
        }
    }

    /**
     * Resuelve la audiencia a usuarios y envia el push a cada uno. Cuenta como enviado al
     * usuario con dispositivos registrados (envio intentado) y como fallido al que no tiene.
     */
    private void deliver(Campaign c) {
        List<UUID> userIds = resolveAudience(c.getAudience());
        Map<String, String> data = c.getRoute() != null ? Map.of("route", c.getRoute()) : Map.of();

        int sent = 0;
        int failed = 0;
        for (UUID userId : userIds) {
            if (deviceTokenRepository.existsByUserId(userId)) {
                pushService.sendToUser(userId, c.getTitle(), c.getBody(), data);
                sent++;
            } else {
                failed++;
            }
        }
        c.setSentCount(sent);
        c.setFailedCount(failed);
        c.setStatus(CampaignStatus.SENT);
        c.setSentAt(OffsetDateTime.now());
        campaignRepository.save(c);
        log.info("Campana {} ({}) enviada: audiencia {}, {} enviados, {} sin dispositivos.",
                c.getId(), c.getTitle(), c.getAudience(), sent, failed);
    }

    /** Traduce la audiencia de la campana a la lista de usuarios destino. */
    private List<UUID> resolveAudience(CampaignAudience audience) {
        return switch (audience) {
            case ALL -> profileRepository.findAllUserIds();
            case TAXISTA -> profileRepository.findUserIdsByClientType(ClientType.TAXISTA);
            case DELIVERY_PEYA -> profileRepository.findUserIdsByClientType(ClientType.DELIVERY_PEYA);
            case SERVICIOS_PROFESIONALES ->
                    profileRepository.findUserIdsByClientType(ClientType.SERVICIOS_PROFESIONALES);
            case ONBOARDING_INCOMPLETE -> profileRepository.findUserIdsWithOnboardingIncomplete();
            case ORIENTATION_PENDING -> profileRepository.findUserIdsByOrientationStatus("PENDING");
        };
    }

    private static CampaignAudience parseAudience(String audience) {
        if (audience == null || audience.isBlank()) {
            throw new BadRequestException("La audiencia de la campana es obligatoria.");
        }
        try {
            return CampaignAudience.valueOf(audience.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Audiencia invalida. Valores permitidos: ALL, TAXISTA, "
                    + "DELIVERY_PEYA, SERVICIOS_PROFESIONALES, ONBOARDING_INCOMPLETE, ORIENTATION_PENDING.");
        }
    }
}
