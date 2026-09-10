package com.sumaup360.app.service;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Regla de completitud de perfil (Linea Personas). El perfil esta completo solo si:
 *  - tiene RUC, validado en SUNAT con estado ACTIVO y condicion HABIDO;
 *  - eligio tipo de cliente (TAXISTA / DELIVERY_PEYA);
 *  - tiene datos basicos: nombres, correo, celular y DNI.
 *
 * Centraliza la regla para que la usen el perfil (updateMe), la validacion SUNAT
 * (refresh RUC/DNI) y el cambio de tipo de cliente.
 */
@Service
public class ProfileCompletionService {

    private final AppUserRepository userRepository;
    private final PersonProfileRepository profileRepository;

    public ProfileCompletionService(AppUserRepository userRepository, PersonProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /** Resultado de la evaluacion: si esta completo, que falta y si el RUC es valido. */
    public record Completion(boolean completed, boolean rucValidoParaPerfil, List<String> missing) {
    }

    /** Evalua y devuelve el estado sin persistir. */
    @Transactional(readOnly = true)
    public Completion status(UUID userId) {
        AppUser u = userRepository.findById(userId).orElse(null);
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        return evaluate(u, p);
    }

    /** Recalcula y persiste profile_completed. Devuelve el estado. */
    @Transactional
    public Completion recompute(UUID userId) {
        AppUser u = userRepository.findById(userId).orElse(null);
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        Completion c = evaluate(u, p);
        if (p != null && p.isProfileCompleted() != c.completed()) {
            p.setProfileCompleted(c.completed());
            profileRepository.save(p);
        }
        return c;
    }

    /** True si el RUC esta ACTIVO y HABIDO (apto para activar funciones tributarias). */
    public static boolean esRucValido(PersonProfile p) {
        return p != null
                && "ACTIVO".equalsIgnoreCase(safe(p.getTaxStatus()))
                && "HABIDO".equalsIgnoreCase(safe(p.getTaxCondition()));
    }

    private Completion evaluate(AppUser u, PersonProfile p) {
        List<String> missing = new ArrayList<>();
        if (p == null || isBlank(p.getRuc())) missing.add("ruc");
        boolean rucValido = esRucValido(p);
        if (!rucValido) missing.add("ruc_validado_sunat");
        if (p == null || p.getClientType() == null) missing.add("tipoCliente");
        if (u == null || isBlank(u.getDisplayName())) missing.add("nombres");
        if (u == null || isBlank(u.getEmail())) missing.add("correo");
        if (u == null || isBlank(u.getPhone())) missing.add("celular");
        if (p == null || isBlank(p.getDni())) missing.add("dni");
        return new Completion(missing.isEmpty(), rucValido, missing);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
