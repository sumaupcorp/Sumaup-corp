package com.sumaup360.app.service;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.dto.FiscalSolDtos.RucFiscalView;
import com.sumaup360.app.dto.FiscalSolDtos.SolCredentialsView;
import com.sumaup360.app.dto.FiscalSolDtos.SolValidationView;
import com.sumaup360.app.dto.FiscalSolDtos.UpsertSolRequest;
import com.sumaup360.app.dto.FiscalSolDtos.ValidateSolRequest;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.sunat.SunatClient;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.security.CryptoService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Credenciales SUNAT (Clave SOL) de la persona. Se guardan en app.person_profile
 * (sol_user, sol_pass_enc). La clave se cifra en reposo con {@link CryptoService} y
 * NUNCA se devuelve en claro a la app: solo el backoffice autorizado puede revelarla.
 */
@Service
public class AppFiscalService {

    private final PersonProfileRepository profileRepository;
    private final CryptoService crypto;
    private final SunatClient sunatClient;
    private final ProfileCompletionService completionService;

    public AppFiscalService(PersonProfileRepository profileRepository, CryptoService crypto,
                            SunatClient sunatClient, ProfileCompletionService completionService) {
        this.profileRepository = profileRepository;
        this.crypto = crypto;
        this.sunatClient = sunatClient;
        this.completionService = completionService;
    }

    @Transactional(readOnly = true)
    public SolCredentialsView get(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        if (p == null) return new SolCredentialsView(null, null, null, null, false);
        return new SolCredentialsView(p.getSolDocMode(), p.getRuc(), p.getSolUser(), p.getDni(),
                p.getSolPassEnc() != null);
    }

    /**
     * Valida la Clave SOL haciendo un login real en SUNAT (via microservicio). No persiste
     * nada: es el paso previo a guardar. Por RUC usa ruc+usuario+clave; por DNI usa dni+clave
     * (el DNI se toma del request o del perfil, resuelto en el diagnostico).
     */
    @Transactional(readOnly = true)
    public SolValidationView validateSol(UUID userId, ValidateSolRequest req) {
        String pass = req.solPass();
        if (pass == null || pass.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tu Clave SOL.");
        }
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        String mode = (req.docMode() == null || req.docMode().isBlank()) ? "ruc" : req.docMode().trim().toLowerCase();

        SunatClient.SolValidation r;
        if ("dni".equals(mode)) {
            String dni = firstNonBlank(req.dni(), p == null ? null : p.getDni());
            if (dni == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "No tenemos tu DNI. Completa tu diagnostico primero.");
            }
            r = sunatClient.validarSol("dni", dni.trim(), null, null, pass);
        } else {
            String ruc = firstNonBlank(req.ruc(), p == null ? null : p.getRuc());
            String usuario = req.solUser();
            if (ruc == null || usuario == null || usuario.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tu RUC y usuario SOL.");
            }
            r = sunatClient.validarSol("ruc", null, ruc.trim(), usuario.trim(), pass);
        }
        String detail = (r.detail() != null && !r.detail().isBlank())
                ? r.detail()
                : (r.ok() ? null : "No pudimos validar tu Clave SOL.");
        return new SolValidationView(r.ok(), r.nombre(), detail);
    }

    @Transactional
    public SolCredentialsView upsert(UUID userId, UpsertSolRequest req) {
        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        String mode = (req.docMode() == null || req.docMode().isBlank()) ? "ruc" : req.docMode().trim().toLowerCase();
        p.setSolDocMode(mode);
        if ("dni".equals(mode)) {
            // Login por DNI + clave: no hay usuario SOL separado.
            if (req.dni() != null) p.setDni(blankToNull(req.dni()));
            p.setSolUser(null);
        } else {
            if (req.ruc() != null) p.setRuc(blankToNull(req.ruc()));
            if (req.solUser() != null) p.setSolUser(blankToNull(req.solUser()));
        }
        // Solo re-cifra si llega una clave nueva no vacia (permite editar sin reescribir la clave).
        if (req.solPass() != null && !req.solPass().isBlank()) {
            p.setSolPassEnc(crypto.encrypt(req.solPass()));
        }
        profileRepository.save(p);
        return get(userId);
    }

    /** Devuelve los datos fiscales (Ficha RUC) ya guardados en el perfil. */
    @Transactional(readOnly = true)
    public RucFiscalView getRucFiscal(UUID userId) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        if (p == null) {
            return new RucFiscalView(null, null, null, null, null, null, null, null, null, null, null, false);
        }
        return toFiscalView(p);
    }

    /**
     * Consulta SUNAT (via microservicio sumaup360-sunat) y guarda los datos de la
     * Ficha RUC en el perfil. Si requestedRuc es null/blank, usa el RUC del perfil.
     */
    @Transactional
    public RucFiscalView refreshRuc(UUID userId, String requestedRuc) {
        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        String ruc = (requestedRuc != null && !requestedRuc.isBlank()) ? requestedRuc.trim() : p.getRuc();
        if (ruc == null || ruc.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Registra tu RUC antes de consultar SUNAT.");
        }
        SunatClient.RucResult r = sunatClient.consultar(ruc);
        if (!r.ok()) {
            throw mapError(r, "No se pudo consultar el RUC en SUNAT.");
        }
        p.setRuc(ruc);
        applyFiscal(p, r.data());
        profileRepository.save(p);
        completionService.recompute(userId); // SUNAT validado puede completar el perfil
        return toFiscalView(p);
    }

    /**
     * Consulta por documento (DNI por defecto) — flujo usual cuando la persona no recuerda
     * su RUC. Resuelve el RUC desde el DNI, guarda los datos de la Ficha RUC en el perfil.
     */
    @Transactional
    public RucFiscalView refreshByDni(UUID userId, String dni, String docType) {
        if (dni == null || dni.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tu numero de documento.");
        }
        SunatClient.RucResult r = sunatClient.consultarPorDni(dni.trim(), docType);
        if (!r.ok()) {
            // 404 = el DNI no tiene RUC registrado (mensaje literal de SUNAT al usuario).
            throw mapError(r, "No se pudo consultar el documento en SUNAT.");
        }
        SunatClient.RucData d = r.data();
        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        if (d.ruc() != null && !d.ruc().isBlank()) {
            p.setRuc(d.ruc());
        }
        applyFiscal(p, d);
        profileRepository.save(p);
        completionService.recompute(userId); // SUNAT validado puede completar el perfil
        return toFiscalView(p);
    }

    /** Traduce un fallo del microservicio a ApiException (404 = sin RUC; el resto 502). */
    private static ApiException mapError(SunatClient.RucResult r, String fallback) {
        HttpStatus status = r.notFound() ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
        String msg = (r.detail() != null && !r.detail().isBlank()) ? r.detail() : fallback;
        return new ApiException(status, msg);
    }

    private static void applyFiscal(PersonProfile p, SunatClient.RucData d) {
        p.setTaxStatus(d.estado());
        p.setTaxCondition(d.condicion());
        p.setTaxpayerType(d.tipoContribuyente());
        p.setEconomicActivity(d.actividadPrincipal());
        p.setCiiuCode(d.ciiu());
        p.setRazonSocial(d.razonSocial());
        p.setFechaInscripcion(d.fechaInscripcion());
        p.setFechaInicioActividades(d.fechaInicioActividades());
        p.setDomicilioFiscal(d.domicilioFiscal());
        p.setSunatRaw(d.rawJson());           // ficha completa literal (analisis/IA)
        p.setRucCheckedAt(OffsetDateTime.now());
    }

    private static RucFiscalView toFiscalView(PersonProfile p) {
        return new RucFiscalView(
                p.getRuc(),
                p.getRazonSocial(),
                p.getTaxStatus(),
                p.getTaxCondition(),
                p.getTaxpayerType(),
                p.getEconomicActivity(),
                p.getCiiuCode(),
                p.getFechaInscripcion(),
                p.getFechaInicioActividades(),
                p.getDomicilioFiscal(),
                p.getRucCheckedAt() == null ? null : p.getRucCheckedAt().toString(),
                ProfileCompletionService.esRucValido(p)
        );
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
