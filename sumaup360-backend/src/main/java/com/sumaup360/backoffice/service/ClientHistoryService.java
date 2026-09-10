package com.sumaup360.backoffice.service;

import com.sumaup360.audit.service.AuditService;
import com.sumaup360.backoffice.domain.ClientHistory;
import com.sumaup360.backoffice.repository.ClientHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Registra y consulta el historial de eventos por cliente (tenant). */
@Service
public class ClientHistoryService {

    private final ClientHistoryRepository repository;
    private final AuditService auditService;

    public ClientHistoryService(ClientHistoryRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public void record(UUID tenantId, String eventType, String description, UUID actorUserId) {
        ClientHistory h = new ClientHistory();
        h.setTenantId(tenantId);
        h.setEventType(eventType);
        h.setDescription(description);
        h.setActorUserId(actorUserId);
        repository.save(h);

        // Espejo en el log de auditoria append-only (fuente inmutable para auditorias futuras).
        auditService.recordAction(actorUserId, "STAFF", "[Cliente] " + description, tenantId);
    }

    @Transactional(readOnly = true)
    public List<ClientHistory> list(UUID tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
