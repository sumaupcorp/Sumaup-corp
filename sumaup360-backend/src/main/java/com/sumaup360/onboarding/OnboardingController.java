package com.sumaup360.onboarding;

import com.sumaup360.security.AppUserPrincipal;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Onboarding self-service para cuentas nuevas. Solo requiere estar autenticado (Firebase);
 * no necesita permisos: el usuario crea su propio negocio (tenant) y queda como admin.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@Tag(name = "Onboarding", description = "Alta self-service del negocio del cliente")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final RucLookupService rucLookupService;

    public OnboardingController(OnboardingService onboardingService, RucLookupService rucLookupService) {
        this.onboardingService = onboardingService;
        this.rucLookupService = rucLookupService;
    }

    public record StatusResponse(boolean onboarded, UUID tenantId) {
    }

    public record OnboardRequest(
            @NotBlank String businessName,
            @NotBlank String businessTypeCode,
            String verticalCode,
            String ruc,
            List<String> branchNames,
            OnboardingService.OperationAnswers operation
    ) {
    }

    public record OnboardResponse(UUID tenantId, UUID companyId) {
    }

    @GetMapping("/status")
    @Operation(summary = "Indica si el usuario ya configuro su negocio")
    public StatusResponse status() {
        AppUserPrincipal p = SecurityUtils.currentPrincipal();
        return new StatusResponse(onboardingService.isOnboarded(p.userId()), p.tenantId());
    }

    @PostMapping
    @Operation(summary = "Configura el negocio del usuario (tenant, empresa, sucursales, plan free)")
    public OnboardResponse onboard(@Valid @RequestBody OnboardRequest req) {
        AppUserPrincipal p = SecurityUtils.currentPrincipal();
        OnboardingService.Result r = onboardingService.onboard(
                p.userId(), p.firebaseUid(), req.businessName(), req.businessTypeCode(),
                req.verticalCode(), req.ruc(), req.branchNames(), req.operation());
        return new OnboardResponse(r.tenantId(), r.companyId());
    }

    @GetMapping("/ruc-lookup")
    @Operation(summary = "Consulta la Ficha RUC en SUNAT para autocompletar el wizard (RUC opcional)")
    public RucLookupService.RucLookupResponse rucLookup(@RequestParam("ruc") String ruc) {
        return rucLookupService.lookup(ruc);
    }
}
