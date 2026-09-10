package com.sumaup360.app.ai;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Administracion (backoffice) de la configuracion y campanas de IA. */
@Service
public class AiAdminService {

    private final AiConfigRepository configRepository;
    private final AiCampaignRepository campaignRepository;

    public AiAdminService(AiConfigRepository configRepository, AiCampaignRepository campaignRepository) {
        this.configRepository = configRepository;
        this.campaignRepository = campaignRepository;
    }

    @Transactional(readOnly = true)
    public AiConfig getConfig() {
        return configRepository.findFirstByOrderByCreatedAtAsc().orElseGet(() -> configRepository.save(new AiConfig()));
    }

    @Transactional
    public AiConfig updateConfig(Integer freeIniciales, Integer freeIa, Integer premiumConsultas,
                                 String periodo, Boolean activo) {
        AiConfig c = getConfig();
        if (freeIniciales != null) c.setFreeIniciales(freeIniciales);
        if (freeIa != null) c.setFreeIa(freeIa);
        if (premiumConsultas != null) c.setPremiumConsultas(premiumConsultas);
        if (periodo != null && !periodo.isBlank()) c.setPeriodo(periodo.trim().toUpperCase());
        if (activo != null) c.setActivo(activo);
        return configRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<AiCampaign> listCampaigns() {
        return campaignRepository.findAll();
    }

    @Transactional
    public AiCampaign createCampaign(String nombre, String clientType, String planCode, int consultasExtra,
                                     LocalDate fechaInicio, LocalDate fechaFin) {
        AiCampaign c = new AiCampaign();
        c.setNombre(nombre);
        c.setClientType(blankToNull(clientType));
        c.setPlanCode(blankToNull(planCode));
        c.setConsultasExtra(consultasExtra);
        c.setFechaInicio(fechaInicio);
        c.setFechaFin(fechaFin);
        c.setActivo(true);
        return campaignRepository.save(c);
    }

    @Transactional
    public AiCampaign setCampaignActive(UUID id, boolean activo) {
        AiCampaign c = campaignRepository.findById(id).orElseThrow();
        c.setActivo(activo);
        return campaignRepository.save(c);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
