package com.sumaup360.tenant.web.dto;

import com.sumaup360.tenant.domain.Company;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** DTOs del recurso Company (empresa). */
public final class CompanyDtos {

    private CompanyDtos() {
    }

    public record CreateCompanyRequest(
            @NotBlank @Size(max = 200) String legalName,
            @Size(max = 11) String ruc,
            @Size(max = 40) String businessTypeCode,
            @Size(max = 40) String verticalCode
    ) {
    }

    public record CompanyResponse(UUID id, UUID tenantId, String legalName, String ruc,
                                  String businessTypeCode, String verticalCode) {
        public static CompanyResponse from(Company c) {
            return new CompanyResponse(c.getId(), c.getTenantId(), c.getLegalName(),
                    c.getRuc(), c.getBusinessTypeCode(), c.getVerticalCode());
        }
    }
}
