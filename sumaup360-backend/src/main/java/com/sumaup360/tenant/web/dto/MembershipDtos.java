package com.sumaup360.tenant.web.dto;

import com.sumaup360.tenant.domain.Membership;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** DTOs del recurso Membership (usuario <-> tenant). */
public final class MembershipDtos {

    private MembershipDtos() {
    }

    public record CreateMembershipRequest(
            @NotBlank String firebaseUid,
            /** Si es null se asume true (primer tenant por defecto del usuario). */
            Boolean asDefault,
            /** Si es true, asigna el rol 'tenant-admin' del tenant (primer admin del negocio). */
            Boolean asAdmin
    ) {
    }

    public record MembershipResponse(UUID id, UUID userId, UUID tenantId,
                                     boolean defaultTenant, String status) {
        public static MembershipResponse from(Membership m) {
            return new MembershipResponse(m.getId(), m.getUserId(), m.getTenantId(),
                    m.isDefaultTenant(), m.getStatus());
        }
    }
}
