package com.sumaup360.backoffice.service;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.service.ProfileCompletionService;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.UserType;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.backoffice.dto.PersonAdminDtos.OrientationStatusView;
import com.sumaup360.backoffice.dto.PersonAdminDtos.PersonRow;
import com.sumaup360.billing.service.SubscriptionService;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Backoffice: listado/busqueda de usuarios de la Linea Personas con su plan. */
@Service
public class PersonAdminService {

    private final AppUserRepository userRepository;
    private final PersonProfileRepository profileRepository;
    private final SubscriptionService subscriptionService;
    private final ProfileCompletionService completionService;

    public PersonAdminService(AppUserRepository userRepository, PersonProfileRepository profileRepository,
                              SubscriptionService subscriptionService, ProfileCompletionService completionService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.subscriptionService = subscriptionService;
        this.completionService = completionService;
    }

    /**
     * Reasigna el rubro/tipo de cliente de una persona (soporte/admin). El usuario no puede
     * cambiarlo por si mismo; solo el staff. Tras cambiarlo se recalcula la completitud del
     * perfil (el tipo de cliente es requisito de perfil completo).
     */
    @Transactional
    public PersonRow changeClientType(UUID userId, String clientTypeStr) {
        AppUser u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        ClientType ct = parseClientType(clientTypeStr);
        if (ct == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de cliente invalido (TAXISTA o DELIVERY_PEYA).");
        }
        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        p.setClientType(ct);
        profileRepository.save(p);
        completionService.recompute(userId);
        return toRow(u);
    }

    /** Rehabilita el intento de diagnostico del usuario (soporte). */
    @Transactional
    public void resetDiagnosis(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado."));
        p.setDiagnosisAvailable(true);
        profileRepository.save(p);
    }

    /**
     * Reactiva la orientacion tributaria del usuario (soporte): vuelve el estado a PENDING
     * para habilitar un nuevo intento. Conserva el resultado anterior como historico hasta
     * que un nuevo analisis lo sobrescriba.
     */
    @Transactional
    public OrientationStatusView resetOrientation(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado."));
        p.setOrientationStatus("PENDING");
        profileRepository.save(p);
        return new OrientationStatusView(userId, p.getOrientationStatus());
    }

    private static ClientType parseClientType(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ClientType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<PersonRow> listPersons(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        return userRepository.findByUserType(UserType.PERSON).stream()
                .map(this::toRow)
                .filter(r -> q.isEmpty() || matches(r, q))
                .toList();
    }

    private PersonRow toRow(AppUser u) {
        PersonProfile p = profileRepository.findByUserId(u.getId()).orElse(null);
        String planCode = subscriptionService.activePlanCode(u.getId());
        boolean premium = subscriptionService.isUserPremium(u.getId());
        return new PersonRow(
                u.getId(), u.getEmail(), u.getDisplayName(),
                p != null ? p.getDni() : null,
                p != null ? p.getRuc() : null,
                (p != null && p.getClientType() != null) ? p.getClientType().name() : null,
                p != null && p.isProfileCompleted(),
                planCode, premium,
                p == null || p.isDiagnosisAvailable(),
                p != null ? p.getOrientationStatus() : null);
    }

    private boolean matches(PersonRow r, String q) {
        return contains(r.email(), q) || contains(r.displayName(), q)
                || contains(r.dni(), q) || contains(r.ruc(), q);
    }

    private static boolean contains(String s, String q) {
        return s != null && s.toLowerCase().contains(q);
    }
}
