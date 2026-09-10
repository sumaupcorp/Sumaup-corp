package com.sumaup360.app.ai;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lectura/edicion del "cerebro" de la IA (system prompts, contexto por caso, parametros). */
@Service
public class AiPromptService {

    private final AiPromptRepository repository;

    public AiPromptService(AiPromptRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AiPrompt get() {
        return repository.findFirstByOrderByCreatedAtAsc().orElseGet(() -> repository.save(new AiPrompt()));
    }

    @Transactional
    public AiPrompt update(String chatSystem, String diagnosisSystem, String taxiContext,
                           String peyaContext, String servContext, Double temperature, Integer maxTokens) {
        AiPrompt p = get();
        if (chatSystem != null && !chatSystem.isBlank()) p.setChatSystem(chatSystem.trim());
        if (diagnosisSystem != null && !diagnosisSystem.isBlank()) p.setDiagnosisSystem(diagnosisSystem.trim());
        if (taxiContext != null) p.setTaxiContext(taxiContext);
        if (peyaContext != null) p.setPeyaContext(peyaContext);
        if (servContext != null) p.setServContext(servContext);
        if (temperature != null) p.setTemperature(Math.max(0.0, Math.min(2.0, temperature)));
        if (maxTokens != null) p.setMaxTokens(Math.max(100, Math.min(4000, maxTokens)));
        return repository.save(p);
    }
}
