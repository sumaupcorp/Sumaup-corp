package com.sumaup360.backoffice.service;

import com.sumaup360.backoffice.domain.FiscalCredentials;
import com.sumaup360.backoffice.dto.FiscalDtos.CredentialsView;
import com.sumaup360.backoffice.dto.FiscalDtos.RevealResponse;
import com.sumaup360.backoffice.dto.FiscalDtos.UpsertCredentialsRequest;
import com.sumaup360.backoffice.repository.FiscalCredentialsRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.security.CryptoService;
import com.sumaup360.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bóveda de credenciales fiscales (Backoffice). La Clave SOL se guarda SIEMPRE cifrada
 * (CryptoService) y solo se descifra en reveal autorizado (auditado por el interceptor).
 */
@Service
public class FiscalCredentialsService {

    private static final Logger log = LoggerFactory.getLogger(FiscalCredentialsService.class);

    private final FiscalCredentialsRepository repository;
    private final TenantRepository tenantRepository;
    private final CryptoService crypto;
    private final ClientHistoryService history;

    public FiscalCredentialsService(FiscalCredentialsRepository repository,
                                    TenantRepository tenantRepository,
                                    CryptoService crypto,
                                    ClientHistoryService history) {
        this.repository = repository;
        this.tenantRepository = tenantRepository;
        this.crypto = crypto;
        this.history = history;
    }

    @Transactional(readOnly = true)
    public CredentialsView get(UUID tenantId) {
        FiscalCredentials c = repository.findByTenantId(tenantId).orElse(null);
        if (c == null) return new CredentialsView(null, null, false);
        return new CredentialsView(c.getRuc(), c.getSolUser(), c.getSolPassEnc() != null);
    }

    @Transactional
    public CredentialsView upsert(UUID tenantId, UpsertCredentialsRequest req, UUID staffId) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente (tenant) no encontrado."));
        FiscalCredentials c = repository.findByTenantId(tenantId).orElseGet(() -> {
            FiscalCredentials n = new FiscalCredentials();
            n.setTenantId(tenantId);
            return n;
        });
        c.setRuc(req.ruc());
        c.setSolUser(req.solUser());
        if (req.solPass() != null && !req.solPass().isBlank()) {
            c.setSolPassEnc(crypto.encrypt(req.solPass())); // cifrado en reposo
        }
        c.setUpdatedBy(staffId);
        repository.save(c);
        history.record(tenantId, "CREDENTIALS_UPDATED", "Credenciales fiscales actualizadas", staffId);
        return new CredentialsView(c.getRuc(), c.getSolUser(), c.getSolPassEnc() != null);
    }

    /** Descifra la Clave SOL (acceso autorizado y auditado). */
    @Transactional
    public RevealResponse reveal(UUID tenantId, UUID staffId) {
        FiscalCredentials c = repository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sin credenciales para este cliente."));
        if (c.getSolPassEnc() == null) {
            throw new ResourceNotFoundException("Este cliente no tiene Clave SOL registrada.");
        }
        log.warn("REVEAL Clave SOL: staff {} accedio a credenciales del tenant {}", staffId, tenantId);
        history.record(tenantId, "SOL_REVEALED", "Se revelo la Clave SOL", staffId);
        return new RevealResponse(c.getSolUser(), crypto.decrypt(c.getSolPassEnc()));
    }
}
