package com.sumaup360.tenant.web.dto;

import com.sumaup360.tenant.domain.Branch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** DTOs del recurso Branch (sucursal). */
public final class BranchDtos {

    private BranchDtos() {
    }

    public record CreateBranchRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 255) String address,
            Boolean main
    ) {
    }

    public record BranchResponse(UUID id, UUID tenantId, UUID companyId,
                                 String name, String address, boolean main) {
        public static BranchResponse from(Branch b) {
            return new BranchResponse(b.getId(), b.getTenantId(), b.getCompanyId(),
                    b.getName(), b.getAddress(), b.isMain());
        }
    }
}
