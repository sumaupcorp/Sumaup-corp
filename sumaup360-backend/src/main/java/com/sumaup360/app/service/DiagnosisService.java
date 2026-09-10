package com.sumaup360.app.service;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.domain.TaxDiagnosis;
import com.sumaup360.app.dto.DiagnosisDtos.DiagnoseRequest;
import com.sumaup360.app.dto.DiagnosisDtos.ProfileRequest;
import com.sumaup360.app.dto.ProfileMeDtos.ProfileMeResponse;
import com.sumaup360.app.dto.ProfileMeDtos.UpdateProfileMeRequest;
import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.repository.TaxDiagnosisRepository;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Diagnostico tributario y perfil de la persona. La recomendacion de plan es una regla
 * simple/provisional (TODO: logica tributaria definitiva).
 */
@Service
public class DiagnosisService {

    private final TaxDiagnosisRepository diagnosisRepository;
    private final PersonProfileRepository profileRepository;
    private final AppUserRepository userRepository;
    private final ProfileCompletionService completionService;

    public DiagnosisService(TaxDiagnosisRepository diagnosisRepository,
                            PersonProfileRepository profileRepository,
                            AppUserRepository userRepository,
                            ProfileCompletionService completionService) {
        this.diagnosisRepository = diagnosisRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.completionService = completionService;
    }

    /** Perfil de usuario (cara persona) combinando AppUser + PersonProfile. */
    @Transactional(readOnly = true)
    public ProfileMeResponse getMe(UUID userId) {
        AppUser u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        return new ProfileMeResponse(
                u.getDisplayName(), u.getEmail(), u.getPhone(), u.getPhotoUrl(),
                p != null ? p.getCountryCode() : "+51",
                p != null ? p.getReferralCode() : null,
                p != null && p.isProfileCompleted(),
                p != null ? p.getSegmentCode() : null,
                p != null ? p.getRuc() : null,
                p != null ? p.getRegime() : null,
                (p != null && p.getClientType() != null) ? p.getClientType().name() : null,
                p != null ? p.getDni() : null,
                p != null ? p.getFirstName() : null,
                p != null ? p.getLastName() : null,
                p != null && p.isOnboardingCompleted(),
                p != null ? p.getOrientationStatus() : null);
    }

    @Transactional
    public ProfileMeResponse updateMe(UUID userId, UpdateProfileMeRequest req) {
        AppUser u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        if (req.phone() != null) u.setPhone(req.phone());
        if (req.photoUrl() != null) u.setPhotoUrl(req.photoUrl());

        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        if (req.firstName() != null) p.setFirstName(req.firstName().trim());
        if (req.lastName() != null) p.setLastName(req.lastName().trim());
        // Nombre para mostrar: prioriza fullName explicito; si no, compone nombre + apellido.
        String displayName = null;
        if (req.fullName() != null && !req.fullName().isBlank()) {
            displayName = req.fullName().trim();
        } else if (p.getFirstName() != null || p.getLastName() != null) {
            displayName = ((p.getFirstName() != null ? p.getFirstName() : "") + " "
                    + (p.getLastName() != null ? p.getLastName() : "")).trim();
        }
        if (displayName != null && !displayName.isBlank()) u.setDisplayName(displayName);
        userRepository.save(u);

        if (req.countryCode() != null) p.setCountryCode(req.countryCode());
        if (req.referralCode() != null) p.setReferralCode(req.referralCode());
        // El onboarding solo se marca completo (nunca se revierte desde el cliente).
        if (Boolean.TRUE.equals(req.onboardingCompleted())) p.setOnboardingCompleted(true);
        // El tipo de cliente se elige UNA sola vez (al crear la cuenta) y luego es inmutable
        // para el usuario: si ya tiene uno, se ignora el del request. Para cambiar de rubro
        // el usuario debe contactar a soporte, que lo reasigna desde el backoffice
        // (PersonAdminService.changeClientType).
        ClientType ct = parseClientType(req.clientType());
        if (ct != null && p.getClientType() == null) p.setClientType(ct);
        if (req.dni() != null && !req.dni().isBlank()) p.setDni(req.dni().trim());
        // La app solo puede marcar la orientacion como PENDING o NOT_REQUIRED; COMPLETED
        // lo pone unicamente el analisis (OrientationService).
        if (req.orientationStatus() != null && !req.orientationStatus().isBlank()) {
            String os = req.orientationStatus().trim().toUpperCase();
            if (!os.equals("PENDING") && !os.equals("NOT_REQUIRED")) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "orientationStatus invalido: solo se acepta PENDING o NOT_REQUIRED.");
            }
            p.setOrientationStatus(os);
        }
        profileRepository.save(p);
        // La completitud ya NO es automatica: depende de RUC validado (ACTIVO+HABIDO),
        // tipo de cliente y datos basicos.
        completionService.recompute(userId);
        return getMe(userId);
    }

    private static ClientType parseClientType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ClientType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional
    public TaxDiagnosis diagnose(UUID userId, DiagnoseRequest req) {
        TaxDiagnosis d = new TaxDiagnosis();
        d.setUserId(userId);
        d.setSegmentCode(req.segmentCode());
        d.setMonthlyIncome(req.monthlyIncome());
        d.setAnswers(req.answers());
        d.setRecommendedPlanCode(recommendPlan(req.segmentCode(), req.monthlyIncome()));
        TaxDiagnosis saved = diagnosisRepository.save(d);

        // Sincroniza el segmento en el perfil de la persona.
        PersonProfile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile p = new PersonProfile();
            p.setUserId(userId);
            return p;
        });
        if (req.segmentCode() != null) {
            profile.setSegmentCode(req.segmentCode());
        }
        profileRepository.save(profile);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<TaxDiagnosis> latest(UUID userId) {
        return diagnosisRepository.findFirstByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public PersonProfile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado. Realiza el diagnostico."));
    }

    @Transactional
    public PersonProfile updateProfile(UUID userId, ProfileRequest req) {
        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        if (req.segmentCode() != null) p.setSegmentCode(req.segmentCode());
        if (req.ruc() != null) p.setRuc(req.ruc());
        if (req.regime() != null) p.setRegime(req.regime());
        return profileRepository.save(p);
    }

    /** Regla provisional de recomendacion de plan. TODO: reemplazar por logica tributaria real. */
    private String recommendPlan(String segmentCode, BigDecimal monthlyIncome) {
        if ("nuevo-rus".equals(segmentCode)) {
            return "basico";
        }
        if (monthlyIncome == null) {
            return "basico";
        }
        if (monthlyIncome.compareTo(new BigDecimal("3000")) < 0) {
            return "basico";
        }
        if (monthlyIncome.compareTo(new BigDecimal("8000")) < 0) {
            return "emprende";
        }
        return "pro";
    }
}
