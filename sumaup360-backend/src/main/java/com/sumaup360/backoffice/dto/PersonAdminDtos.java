package com.sumaup360.backoffice.dto;

import com.sumaup360.app.ai.AiCampaign;
import com.sumaup360.app.ai.AiConfig;

import java.time.LocalDate;
import java.util.UUID;

/** DTOs de administracion backoffice de planes premium y de la IA (Linea Personas). */
public final class PersonAdminDtos {

    private PersonAdminDtos() {
    }

    /** Fila de persona en el listado del backoffice. */
    public record PersonRow(UUID userId, String email, String displayName, String dni, String ruc,
                            String clientType, boolean profileCompleted, String planCode, boolean premium,
                            boolean diagnosisAvailable, String orientationStatus) {
    }

    /** Estado de la orientacion tributaria de una persona tras un reset de soporte. */
    public record OrientationStatusView(UUID userId, String orientationStatus) {
    }

    public record ActivatePremiumRequest(String planCode, Integer months) {
    }

    /** Reasignacion de rubro por staff (soporte/admin): TAXISTA o DELIVERY_PEYA. */
    public record UpdateClientTypeRequest(String clientType) {
    }

    public record PersonPlanView(String planCode, boolean premium) {
    }

    public record AiUsageView(int used, int limit, int remaining, boolean blocked, String periodo, boolean premium) {
    }

    public record AiConfigView(int freeIniciales, int freeIa, int premiumConsultas, String periodo, boolean activo) {
        public static AiConfigView from(AiConfig c) {
            return new AiConfigView(c.getFreeIniciales(), c.getFreeIa(), c.getPremiumConsultas(),
                    c.getPeriodo(), c.isActivo());
        }
    }

    public record UpdateAiConfigRequest(Integer freeIniciales, Integer freeIa, Integer premiumConsultas,
                                        String periodo, Boolean activo) {
    }

    /** "Cerebro" de la IA: system prompts, contexto por caso y parametros (editable). */
    public record AiPromptView(String chatSystem, String diagnosisSystem, String taxiContext,
                               String peyaContext, String servContext, double temperature, int maxTokens) {
        public static AiPromptView from(com.sumaup360.app.ai.AiPrompt p) {
            return new AiPromptView(p.getChatSystem(), p.getDiagnosisSystem(), p.getTaxiContext(),
                    p.getPeyaContext(), p.getServContext(), p.getTemperature(), p.getMaxTokens());
        }
    }

    public record UpdateAiPromptRequest(String chatSystem, String diagnosisSystem, String taxiContext,
                                        String peyaContext, String servContext, Double temperature, Integer maxTokens) {
    }

    public record AiCampaignView(UUID id, String nombre, String clientType, String planCode, int consultasExtra,
                                 LocalDate fechaInicio, LocalDate fechaFin, boolean activo) {
        public static AiCampaignView from(AiCampaign c) {
            return new AiCampaignView(c.getId(), c.getNombre(), c.getClientType(), c.getPlanCode(),
                    c.getConsultasExtra(), c.getFechaInicio(), c.getFechaFin(), c.isActivo());
        }
    }

    public record CreateCampaignRequest(String nombre, String clientType, String planCode, int consultasExtra,
                                        LocalDate fechaInicio, LocalDate fechaFin) {
    }
}
