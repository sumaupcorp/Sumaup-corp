package com.sumaup360.erp.web.dto;

import com.sumaup360.erp.domain.Supplier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class SupplierDtos {

    private SupplierDtos() {
    }

    public record CreateSupplierRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 11) String ruc,
            @Size(max = 30) String phone,
            @Size(max = 255) String email,
            @Size(max = 120) String contactName,
            @Size(max = 255) String address,
            @Size(max = 500) String notes
    ) {
    }

    public record UpdateSupplierRequest(
            @Size(max = 200) String name,
            @Size(max = 11) String ruc,
            @Size(max = 30) String phone,
            @Size(max = 255) String email,
            @Size(max = 120) String contactName,
            @Size(max = 255) String address,
            @Size(max = 500) String notes,
            Boolean active
    ) {
    }

    public record SupplierResponse(UUID id, String name, String ruc, String phone, String email,
                                   String contactName, String address, String notes, boolean active) {
        public static SupplierResponse from(Supplier s) {
            return new SupplierResponse(s.getId(), s.getName(), s.getRuc(), s.getPhone(),
                    s.getEmail(), s.getContactName(), s.getAddress(), s.getNotes(), s.isActive());
        }
    }
}
