package com.sumaup360.app.ai;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.billing.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Limites y consumo de IA. Los limites NO estan hardcodeados: vienen de app.ai_config
 * (editable desde backoffice) + campanas temporales (app.ai_campaign).
 */
@Service
public class AiUsageService {

    private final AiConfigRepository configRepository;
    private final AiCampaignRepository campaignRepository;
    private final AiUsageLogRepository usageLogRepository;
    private final SubscriptionService subscriptionService;
    private final PersonProfileRepository profileRepository;

    public AiUsageService(AiConfigRepository configRepository, AiCampaignRepository campaignRepository,
                          AiUsageLogRepository usageLogRepository, SubscriptionService subscriptionService,
                          PersonProfileRepository profileRepository) {
        this.configRepository = configRepository;
        this.campaignRepository = campaignRepository;
        this.usageLogRepository = usageLogRepository;
        this.subscriptionService = subscriptionService;
        this.profileRepository = profileRepository;
    }

    public record UsageStatus(int used, int limit, int remaining, boolean blocked, String periodo, boolean premium) {
    }

    @Transactional(readOnly = true)
    public UsageStatus status(UUID userId) {
        AiConfig cfg = config();
        boolean premium = subscriptionService.isUserPremium(userId);
        int base = premium ? cfg.getPremiumConsultas() : cfg.getFreeIa();
        int limit = base + campaignExtra(userId, premium);
        OffsetDateTime since = periodStart(cfg.getPeriodo());
        int used = (int) usageLogRepository.countByUserIdAndConsumioCreditoTrueAndCreatedAtAfter(userId, since);
        int remaining = Math.max(0, limit - used);
        return new UsageStatus(used, limit, remaining, remaining <= 0, cfg.getPeriodo(), premium);
    }

    @Transactional
    public void record(UUID userId, String pregunta, String respuesta, String modelo, Integer tokens,
                       String tipoConsulta, boolean consumioCredito, String planActivo) {
        AiUsageLog log = new AiUsageLog();
        log.setUserId(userId);
        log.setPregunta(trim(pregunta, 4000));
        log.setRespuesta(trim(respuesta, 8000));
        log.setModelo(modelo);
        log.setTokens(tokens);
        log.setTipoConsulta(tipoConsulta);
        log.setConsumioCredito(consumioCredito);
        log.setPlanActivo(planActivo);
        usageLogRepository.save(log);
    }

    public AiConfig config() {
        return configRepository.findFirstByOrderByCreatedAtAsc().orElseGet(AiConfig::new);
    }

    private int campaignExtra(UUID userId, boolean premium) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        String ct = (p != null && p.getClientType() != null) ? p.getClientType().name() : null;
        String planCode = subscriptionService.activePlanCode(userId);
        LocalDate today = LocalDate.now();
        int extra = 0;
        for (AiCampaign c : campaignRepository.findByActivoTrue()) {
            if (c.getFechaInicio() != null && c.getFechaInicio().isAfter(today)) continue;
            if (c.getFechaFin() != null && c.getFechaFin().isBefore(today)) continue;
            if (c.getClientType() != null && !c.getClientType().equalsIgnoreCase(ct)) continue;
            if (c.getPlanCode() != null && !c.getPlanCode().equalsIgnoreCase(planCode)) continue;
            extra += c.getConsultasExtra();
        }
        return extra;
    }

    private OffsetDateTime periodStart(String periodo) {
        OffsetDateTime now = OffsetDateTime.now();
        if ("DIARIO".equalsIgnoreCase(periodo)) {
            return now.toLocalDate().atStartOfDay().atOffset(now.getOffset());
        }
        return now.toLocalDate().withDayOfMonth(1).atStartOfDay().atOffset(now.getOffset());
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
