package com.sumaup360.app.aidiagnosis;

import com.sumaup360.app.ai.AiPrompt;
import com.sumaup360.app.ai.AiPromptService;
import com.sumaup360.app.ai.LlmClient;
import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.dto.SummaryDtos.SummaryResponse;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.service.AppFiscalService;
import com.sumaup360.app.service.SummaryService;
import com.sumaup360.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Diagnostico tributario con IA (Gemini). Flujo: el usuario ingresa su DNI o RUC, se consulta
 * la Ficha RUC en SUNAT (microservicio Python) y se guarda en su perfil; con esos datos + el
 * tipo de trabajador (taxista/repartidor/profesional) la IA recomienda el regimen adecuado.
 *
 * Es GRATIS pero de UN solo intento por cuenta (se apaga diagnosisAvailable al generarlo).
 * Soporte lo reactiva desde el backoffice. Guarda historico como reporte.
 */
@Service
public class DiagnosisReportService {

    private final DiagnosisReportRepository reportRepository;
    private final PersonProfileRepository profileRepository;
    private final SummaryService summaryService;
    private final LlmClient llm;
    private final AppFiscalService fiscalService;
    private final AiPromptService aiPromptService;

    public DiagnosisReportService(DiagnosisReportRepository reportRepository, PersonProfileRepository profileRepository,
                                  SummaryService summaryService, LlmClient llm, AppFiscalService fiscalService,
                                  AiPromptService aiPromptService) {
        this.reportRepository = reportRepository;
        this.profileRepository = profileRepository;
        this.summaryService = summaryService;
        this.llm = llm;
        this.fiscalService = fiscalService;
        this.aiPromptService = aiPromptService;
    }

    /** Genera el diagnostico: consulta SUNAT por doc, guarda la ficha y llama a la IA. */
    @Transactional
    public DiagnosisReport generate(UUID userId, String doc, String docType) {
        PersonProfile existing = profileRepository.findByUserId(userId).orElse(null);
        if (existing != null && !existing.isDiagnosisAvailable()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Ya realizaste tu diagnostico. Escribe a soporte para habilitar uno nuevo.");
        }
        if (doc == null || doc.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tu DNI o RUC.");
        }
        // 1) Consulta la Ficha RUC en SUNAT y guarda los datos fiscales en el perfil.
        String dt = docType == null ? "" : docType.trim().toUpperCase();
        if (dt.equals("RUC")) {
            fiscalService.refreshRuc(userId, doc.trim());
        } else {
            fiscalService.refreshByDni(userId, doc.trim(), dt.isEmpty() ? "DNI" : dt);
        }

        // 2) Contexto (ya con la ficha fresca) + IA. System prompt configurable desde backoffice.
        String contexto = buildContext(userId);
        AiPrompt prompt = aiPromptService.get();
        String system = prompt.getDiagnosisSystem();
        String caseContext = prompt.contextFor(existing != null ? existing.getClientType() : null);
        if (!caseContext.isBlank()) {
            system = system + "\n\n" + caseContext;
        }
        Optional<String> ai = llm.generate(system, "Diagnostica a esta persona:\n" + contexto,
                prompt.getTemperature(), prompt.getMaxTokens());
        String resultado = ai.orElseThrow(() -> new ApiException(HttpStatus.BAD_GATEWAY,
                "El diagnostico con IA no esta disponible ahora. Intenta mas tarde."));

        // 3) Guarda el reporte y marca el intento como usado.
        DiagnosisReport r = new DiagnosisReport();
        r.setUserId(userId);
        r.setContexto(contexto);
        r.setResultado(resultado);
        r.setModelo(llm.model());
        reportRepository.save(r);

        PersonProfile p = profileRepository.findByUserId(userId).orElseThrow();
        p.setDiagnosisAvailable(false);
        profileRepository.save(p);
        return r;
    }

    /** Estado del diagnostico: si el usuario aun tiene su intento disponible y su ultimo reporte. */
    @Transactional(readOnly = true)
    public DiagnosisStatus status(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        boolean available = p == null || p.isDiagnosisAvailable();
        DiagnosisReport latest = reportRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().findFirst().orElse(null);
        return new DiagnosisStatus(available, latest);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisReport> reports(UUID userId) {
        return reportRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Estado del diagnostico para la app. */
    public record DiagnosisStatus(boolean available, DiagnosisReport latest) {
    }

    private String buildContext(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        SummaryResponse s = summaryService.monthly(userId, YearMonth.now());
        StringBuilder sb = new StringBuilder();
        if (p != null) {
            sb.append("Tipo de trabajador: ").append(clientTypeLabel(p)).append("\n");
            sb.append("Razon social: ").append(nz(p.getRazonSocial())).append("\n");
            sb.append("RUC: ").append(nz(p.getRuc())).append("\n");
            sb.append("Tipo de contribuyente: ").append(nz(p.getTaxpayerType())).append("\n");
            sb.append("Estado SUNAT: ").append(nz(p.getTaxStatus())).append("\n");
            sb.append("Condicion SUNAT: ").append(nz(p.getTaxCondition())).append("\n");
            sb.append("Actividad economica: ").append(nz(p.getEconomicActivity())).append("\n");
        }
        sb.append("Ingresos del mes: S/ ").append(s.totalIncome()).append("\n");
        sb.append("Gastos del mes: S/ ").append(s.totalExpense()).append("\n");
        sb.append("Utilidad del mes: S/ ").append(s.utility()).append("\n");
        // Ficha RUC completa de SUNAT (para un analisis mas preciso). Truncada por seguridad.
        if (p != null && p.getSunatRaw() != null && !p.getSunatRaw().isBlank()) {
            String raw = p.getSunatRaw();
            if (raw.length() > 2500) raw = raw.substring(0, 2500);
            sb.append("Ficha RUC (SUNAT): ").append(raw).append("\n");
        }
        return sb.toString();
    }

    private static String clientTypeLabel(PersonProfile p) {
        if (p.getClientType() == null) return "desconocido";
        return switch (p.getClientType()) {
            case TAXISTA -> "Taxista";
            case DELIVERY_PEYA -> "Repartidor de delivery";
            case SERVICIOS_PROFESIONALES -> "Profesional independiente (recibos por honorarios)";
        };
    }

    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "no registrado" : s;
    }
}
