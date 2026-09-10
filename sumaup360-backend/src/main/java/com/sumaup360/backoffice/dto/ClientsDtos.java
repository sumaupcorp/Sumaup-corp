package com.sumaup360.backoffice.dto;

import com.sumaup360.backoffice.domain.ClientHistory;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ClientsDtos {

    private ClientsDtos() {
    }

    /** Evento del historial de un cliente. */
    public record HistoryEvent(UUID id, String eventType, String description, UUID actorUserId,
                               String actorName, OffsetDateTime at) {
        public static HistoryEvent from(ClientHistory h, String actorName) {
            return new HistoryEvent(h.getId(), h.getEventType(), h.getDescription(), h.getActorUserId(),
                    actorName, h.getCreatedAt());
        }
    }

    /** Resumen de un cliente (tenant) para el panorama del Backoffice. */
    public record ClientView(
            UUID tenantId,
            String code,
            String name,
            String status,
            int companies,
            int branches,
            int users,
            String planCode
    ) {
    }
}
