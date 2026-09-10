package com.sumaup360.backoffice.web;

import com.sumaup360.app.ai.AiAdminService;
import com.sumaup360.app.ai.AiPromptService;
import com.sumaup360.app.ai.AiUsageService;
import com.sumaup360.backoffice.dto.PersonAdminDtos.ActivatePremiumRequest;
import com.sumaup360.backoffice.dto.PersonAdminDtos.AiCampaignView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.AiConfigView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.AiPromptView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.AiUsageView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.CreateCampaignRequest;
import com.sumaup360.backoffice.dto.PersonAdminDtos.OrientationStatusView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.PersonPlanView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.PersonRow;
import com.sumaup360.backoffice.dto.PersonAdminDtos.UpdateAiConfigRequest;
import com.sumaup360.backoffice.dto.PersonAdminDtos.UpdateAiPromptRequest;
import com.sumaup360.backoffice.dto.PersonAdminDtos.UpdateClientTypeRequest;
import com.sumaup360.backoffice.service.PersonAdminService;
import com.sumaup360.billing.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Backoffice: administracion de planes premium (activacion manual) y de la IA
 * (limites configurables, campanas y consumo) para la Linea Personas.
 */
@RestController
@RequestMapping("/api/v1/backoffice/persons-admin")
@Tag(name = "Backoffice - Personas (planes e IA)", description = "Activacion premium y administracion de IA")
public class BackofficePersonAdminController {

    private final SubscriptionService subscriptionService;
    private final AiUsageService aiUsageService;
    private final AiAdminService aiAdminService;
    private final AiPromptService aiPromptService;
    private final PersonAdminService personAdminService;

    public BackofficePersonAdminController(SubscriptionService subscriptionService,
                                           AiUsageService aiUsageService, AiAdminService aiAdminService,
                                           AiPromptService aiPromptService, PersonAdminService personAdminService) {
        this.subscriptionService = subscriptionService;
        this.aiUsageService = aiUsageService;
        this.aiAdminService = aiAdminService;
        this.aiPromptService = aiPromptService;
        this.personAdminService = personAdminService;
    }

    // --- Listado de personas ---

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ai:usage:read')")
    @Operation(summary = "Lista/busca usuarios de la Linea Personas con su plan")
    public List<PersonRow> users(@RequestParam(required = false) String query) {
        return personAdminService.listPersons(query);
    }

    // --- Rubro / tipo de cliente ---

    @PostMapping("/{userId}/client-type")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Reasigna el rubro/tipo de cliente de una persona (soporte/admin)")
    public PersonRow changeClientType(@PathVariable UUID userId, @Valid @RequestBody UpdateClientTypeRequest req) {
        return personAdminService.changeClientType(userId, req.clientType());
    }

    @PostMapping("/{userId}/diagnosis/reset")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Rehabilita el intento de diagnostico con IA de una persona (soporte)")
    public void resetDiagnosis(@PathVariable UUID userId) {
        personAdminService.resetDiagnosis(userId);
    }

    @PostMapping("/{userId}/orientation/reset")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Reactiva la orientacion tributaria de una persona (soporte)")
    public OrientationStatusView resetOrientation(@PathVariable UUID userId) {
        return personAdminService.resetOrientation(userId);
    }

    // --- Plan premium ---

    @GetMapping("/{userId}/plan")
    @PreAuthorize("hasAuthority('ai:usage:read')")
    @Operation(summary = "Plan actual del usuario")
    public PersonPlanView plan(@PathVariable UUID userId) {
        return new PersonPlanView(subscriptionService.activePlanCode(userId), subscriptionService.isUserPremium(userId));
    }

    @PostMapping("/{userId}/premium")
    @PreAuthorize("hasAuthority('person:plan:manage')")
    @Operation(summary = "Activa premium a una persona (manual)")
    public PersonPlanView activatePremium(@PathVariable UUID userId, @Valid @RequestBody ActivatePremiumRequest req) {
        subscriptionService.activatePremium(userId, req.planCode(), req.months() == null ? 1 : req.months());
        return new PersonPlanView(subscriptionService.activePlanCode(userId), subscriptionService.isUserPremium(userId));
    }

    @PostMapping("/{userId}/premium/cancel")
    @PreAuthorize("hasAuthority('person:plan:manage')")
    @Operation(summary = "Cancela la suscripcion activa (vuelve a free)")
    public PersonPlanView cancelPremium(@PathVariable UUID userId) {
        subscriptionService.cancelUserActive(userId);
        return new PersonPlanView(subscriptionService.activePlanCode(userId), subscriptionService.isUserPremium(userId));
    }

    // --- Consumo de IA ---

    @GetMapping("/{userId}/ai-usage")
    @PreAuthorize("hasAuthority('ai:usage:read')")
    @Operation(summary = "Consumo de IA del usuario")
    public AiUsageView aiUsage(@PathVariable UUID userId) {
        AiUsageService.UsageStatus u = aiUsageService.status(userId);
        return new AiUsageView(u.used(), u.limit(), u.remaining(), u.blocked(), u.periodo(), u.premium());
    }

    // --- Configuracion de IA ---

    @GetMapping("/ai/config")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Configuracion de limites de IA")
    public AiConfigView getAiConfig() {
        return AiConfigView.from(aiAdminService.getConfig());
    }

    @PutMapping("/ai/config")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Actualiza limites de IA (free/premium, periodo)")
    public AiConfigView updateAiConfig(@RequestBody UpdateAiConfigRequest req) {
        return AiConfigView.from(aiAdminService.updateConfig(
                req.freeIniciales(), req.freeIa(), req.premiumConsultas(), req.periodo(), req.activo()));
    }

    // --- "Cerebro" de la IA (system prompts, contexto por caso, parametros) ---

    @GetMapping("/ai/prompt")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Configuracion del cerebro de la IA (prompts y contexto por caso)")
    public AiPromptView getAiPrompt() {
        return AiPromptView.from(aiPromptService.get());
    }

    @PutMapping("/ai/prompt")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Edita el system prompt, el contexto por caso y los parametros de la IA")
    public AiPromptView updateAiPrompt(@RequestBody UpdateAiPromptRequest req) {
        return AiPromptView.from(aiPromptService.update(
                req.chatSystem(), req.diagnosisSystem(), req.taxiContext(),
                req.peyaContext(), req.servContext(), req.temperature(), req.maxTokens()));
    }

    // --- Campanas ---

    @GetMapping("/ai/campaigns")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Lista campanas de IA")
    public List<AiCampaignView> campaigns() {
        return aiAdminService.listCampaigns().stream().map(AiCampaignView::from).toList();
    }

    @PostMapping("/ai/campaigns")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Crea una campana de IA (consultas extra temporales)")
    public AiCampaignView createCampaign(@Valid @RequestBody CreateCampaignRequest req) {
        return AiCampaignView.from(aiAdminService.createCampaign(
                req.nombre(), req.clientType(), req.planCode(), req.consultasExtra(),
                req.fechaInicio(), req.fechaFin()));
    }

    @PutMapping("/ai/campaigns/{id}/active")
    @PreAuthorize("hasAuthority('ai:manage')")
    @Operation(summary = "Activa/desactiva una campana")
    public AiCampaignView setCampaignActive(@PathVariable UUID id, @RequestParam boolean value) {
        return AiCampaignView.from(aiAdminService.setCampaignActive(id, value));
    }
}
