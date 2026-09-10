package com.sumaup360.tenant.web.dto;

import com.sumaup360.tenant.domain.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** DTOs del recurso Tenant. */
public final class TenantDtos {

    private TenantDtos() {
    }

    public record CreateTenantRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 160) String name
    ) {
    }

    public record TenantResponse(UUID id, String code, String name, String status) {
        public static TenantResponse from(Tenant t) {
            return new TenantResponse(t.getId(), t.getCode(), t.getName(), t.getStatus());
        }
    }
}
