package com.sumaup360.backoffice.dto;

import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class StaffDtos {

    private StaffDtos() {
    }

    public record CreateStaffRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6) String password,
            @NotBlank String name,
            @NotBlank String roleCode   // gerencia, contador, desarrollador, soporte, logistica, admin
    ) {
    }

    public record StaffView(UUID userId, String email, String name, List<String> roles) {
        public static StaffView from(AppUser u) {
            return new StaffView(
                    u.getId(), u.getEmail(), u.getDisplayName(),
                    u.getRoles().stream().filter(r -> r.getTenantId() == null).map(Role::getCode).toList());
        }
    }
}
