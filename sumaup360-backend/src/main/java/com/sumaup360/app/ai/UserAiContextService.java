package com.sumaup360.app.ai;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.dto.SummaryDtos.SummaryResponse;
import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.honorarios.repository.HonorarioRequestRepository;
import com.sumaup360.app.honorarios.repository.SuspensionRequestRepository;
import com.sumaup360.app.peya.repository.PeyaUploadRepository;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.service.SummaryService;
import com.sumaup360.app.taxi.repository.ReceiptRequestRepository;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.billing.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

/**
 * Arma el contexto completo del usuario (perfil + plan + estadisticas + conteos) para
 * inyectarlo a la IA. Asi el asistente puede responder "cuantos comprobantes tengo",
 * "que plan tengo", "cual es mi DNI", "mis estadisticas del mes", etc.
 */
@Service
public class UserAiContextService {

    private final AppUserRepository userRepository;
    private final PersonProfileRepository profileRepository;
    private final SubscriptionService subscriptionService;
    private final SummaryService summaryService;
    private final ReceiptRequestRepository receiptRepository;
    private final PeyaUploadRepository peyaRepository;
    private final HonorarioRequestRepository honorarioRepository;
    private final SuspensionRequestRepository suspensionRepository;

    public UserAiContextService(AppUserRepository userRepository, PersonProfileRepository profileRepository,
                                SubscriptionService subscriptionService, SummaryService summaryService,
                                ReceiptRequestRepository receiptRepository, PeyaUploadRepository peyaRepository,
                                HonorarioRequestRepository honorarioRepository,
                                SuspensionRequestRepository suspensionRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.subscriptionService = subscriptionService;
        this.summaryService = summaryService;
        this.receiptRepository = receiptRepository;
        this.peyaRepository = peyaRepository;
        this.honorarioRepository = honorarioRepository;
        this.suspensionRepository = suspensionRepository;
    }

    @Transactional(readOnly = true)
    public String build(UUID userId) {
        AppUser u = userRepository.findById(userId).orElse(null);
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        SummaryResponse s = summaryService.monthly(userId, YearMonth.now());
        String plan = subscriptionService.activePlanCode(userId);
        boolean premium = subscriptionService.isUserPremium(userId);
        ClientType ct = p != null ? p.getClientType() : null;

        StringBuilder sb = new StringBuilder();
        sb.append("DATOS DEL USUARIO (usalos para responder con precision; no los inventes):\n");
        if (u != null) {
            sb.append("- Nombre: ").append(nz(u.getDisplayName())).append("\n");
            sb.append("- Correo: ").append(nz(u.getEmail())).append("\n");
            sb.append("- Celular: ").append(nz(u.getPhone())).append("\n");
        }
        if (p != null) {
            sb.append("- DNI: ").append(nz(p.getDni())).append("\n");
            sb.append("- RUC: ").append(nz(p.getRuc())).append("\n");
            sb.append("- Tipo de trabajador: ").append(label(ct)).append("\n");
            sb.append("- Razon social: ").append(nz(p.getRazonSocial())).append("\n");
            sb.append("- Estado SUNAT: ").append(nz(p.getTaxStatus()))
                    .append(" / ").append(nz(p.getTaxCondition())).append("\n");
            sb.append("- Actividad economica: ").append(nz(p.getEconomicActivity())).append("\n");
        }
        sb.append("- Plan: ").append(plan == null ? "Free" : plan).append(premium ? " (Premium)" : " (Free)").append("\n");
        sb.append("- Ingresos del mes: S/ ").append(s.totalIncome()).append("\n");
        sb.append("- Gastos del mes: S/ ").append(s.totalExpense()).append("\n");
        sb.append("- Utilidad del mes: S/ ").append(s.utility()).append("\n");

        if (ct == ClientType.TAXISTA) {
            sb.append("- Solicitudes de comprobante recibidas: ")
                    .append(receiptRepository.countByTaxistaUserId(userId)).append("\n");
        } else if (ct == ClientType.DELIVERY_PEYA) {
            sb.append("- Cargas de ventas (Peya) subidas: ")
                    .append(peyaRepository.countByUserId(userId)).append("\n");
        } else if (ct == ClientType.SERVICIOS_PROFESIONALES) {
            sb.append("- Recibos por honorarios solicitados: ")
                    .append(honorarioRepository.countByUserId(userId)).append("\n");
            sb.append("- Suspensiones de 4ta solicitadas: ")
                    .append(suspensionRepository.countByUserId(userId)).append("\n");
        }
        return sb.toString();
    }

    private static String label(ClientType ct) {
        if (ct == null) return "desconocido";
        return switch (ct) {
            case TAXISTA -> "Taxista";
            case DELIVERY_PEYA -> "Repartidor de delivery";
            case SERVICIOS_PROFESIONALES -> "Profesional independiente (recibos por honorarios)";
        };
    }

    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "no registrado" : s;
    }
}
