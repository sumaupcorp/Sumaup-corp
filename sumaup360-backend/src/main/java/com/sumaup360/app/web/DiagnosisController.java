package com.sumaup360.app.web;

import com.sumaup360.app.dto.DiagnosisDtos.DiagnoseRequest;
import com.sumaup360.app.dto.DiagnosisDtos.DiagnosisResponse;
import com.sumaup360.app.dto.DiagnosisDtos.ProfileRequest;
import com.sumaup360.app.dto.DiagnosisDtos.ProfileResponse;
import com.sumaup360.app.dto.ProfileMeDtos.ProfileCompletionResponse;
import com.sumaup360.app.dto.ProfileMeDtos.ProfileMeResponse;
import com.sumaup360.app.dto.ProfileMeDtos.UpdateProfileMeRequest;
import com.sumaup360.app.service.DiagnosisService;
import com.sumaup360.app.service.ProfileCompletionService;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Diagnostico tributario y perfil de la persona. Linea Personas: scopeado por usuario,
 * sin permiso especial (solo autenticado).
 */
@RestController
@RequestMapping("/api/v1/app")
@Tag(name = "Personas - Diagnostico", description = "Diagnostico tributario y perfil de la persona")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final ProfileCompletionService completionService;

    public DiagnosisController(DiagnosisService diagnosisService, ProfileCompletionService completionService) {
        this.diagnosisService = diagnosisService;
        this.completionService = completionService;
    }

    @PostMapping("/diagnosis")
    @Operation(summary = "Registra un diagnostico y recomienda un plan")
    public DiagnosisResponse diagnose(@Valid @RequestBody DiagnoseRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return DiagnosisResponse.from(diagnosisService.diagnose(userId, req));
    }

    @GetMapping("/diagnosis/latest")
    @Operation(summary = "Devuelve el ultimo diagnostico de la persona")
    public DiagnosisResponse latest() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return diagnosisService.latest(userId).map(DiagnosisResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Aun no tienes un diagnostico."));
    }

    @GetMapping("/profile")
    @Operation(summary = "Perfil tributario de la persona")
    public ProfileResponse getProfile() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ProfileResponse.from(diagnosisService.getProfile(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "Actualiza el perfil tributario de la persona")
    public ProfileResponse updateProfile(@Valid @RequestBody ProfileRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ProfileResponse.from(diagnosisService.updateProfile(userId, req));
    }

    @GetMapping("/profile/me")
    @Operation(summary = "Perfil de usuario (nombre, telefono, foto) de la persona")
    public ProfileMeResponse getMe() {
        return diagnosisService.getMe(SecurityUtils.currentPrincipal().userId());
    }

    @PutMapping("/profile/me")
    @Operation(summary = "Actualiza el perfil de usuario (completar perfil)")
    public ProfileMeResponse updateMe(@Valid @RequestBody UpdateProfileMeRequest req) {
        return diagnosisService.updateMe(SecurityUtils.currentPrincipal().userId(), req);
    }

    @GetMapping("/profile/completion")
    @Operation(summary = "Estado de completitud del perfil (que falta para activar la app)")
    public ProfileCompletionResponse completion() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        ProfileCompletionService.Completion c = completionService.status(userId);
        return new ProfileCompletionResponse(c.completed(), c.rucValidoParaPerfil(), c.missing());
    }
}
