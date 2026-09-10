package com.sumaup360.audit.service;

import com.sumaup360.audit.domain.AuditEvent;
import com.sumaup360.audit.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Registro y consulta de eventos de auditoria. */
@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** Registra en una transaccion propia para no afectar la operacion principal. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID actorUserId, String actorType, String method, String path,
                       int status, UUID tenantId, String ip) {
        AuditEvent e = new AuditEvent();
        e.setOccurredAt(OffsetDateTime.now());
        e.setActorUserId(actorUserId);
        e.setActorType(actorType);
        e.setMethod(method);
        e.setPath(path);
        e.setAction(method + " " + path);
        e.setStatus(status);
        e.setTenantId(tenantId);
        e.setIp(ip);
        repository.save(e);
    }

    /**
     * Registra un evento de negocio legible (no HTTP) en el log de auditoria append-only.
     * Util para acciones sensibles del Backoffice (cambios de plan, credenciales, etc.).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAction(UUID actorUserId, String actorType, String action, UUID tenantId) {
        AuditEvent e = new AuditEvent();
        e.setOccurredAt(OffsetDateTime.now());
        e.setActorUserId(actorUserId);
        e.setActorType(actorType);
        e.setAction(action.length() > 160 ? action.substring(0, 160) : action);
        e.setStatus(200);
        e.setTenantId(tenantId);
        repository.save(e);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> recent() {
        return repository.findTop100ByOrderByOccurredAtDesc();
    }
}
