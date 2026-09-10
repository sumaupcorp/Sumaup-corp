package com.sumaup360.audit.web;

import com.sumaup360.audit.domain.AuditEvent;
import com.sumaup360.audit.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Consulta de auditoria (staff). */
@RestController
@RequestMapping("/api/v1/backoffice/audit")
@Tag(name = "Backoffice - Auditoria", description = "Bitacora de eventos del sistema")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    public record AuditEventView(UUID id, OffsetDateTime occurredAt, UUID actorUserId,
                                 String actorType, String action, Integer status, UUID tenantId) {
        static AuditEventView from(AuditEvent e) {
            return new AuditEventView(e.getId(), e.getOccurredAt(), e.getActorUserId(),
                    e.getActorType(), e.getAction(), e.getStatus(), e.getTenantId());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    @Operation(summary = "Ultimos eventos de auditoria (requiere audit:read)")
    public List<AuditEventView> recent() {
        return auditService.recent().stream().map(AuditEventView::from).toList();
    }
}
