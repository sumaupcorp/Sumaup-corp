package com.sumaup360.tenant.web.dto;

import com.sumaup360.auth.domain.Permission;
import com.sumaup360.auth.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** DTOs de roles propios del tenant. */
public final class RoleDtos {

    private RoleDtos() {
    }

    public record CreateRoleRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 120) String name,
            List<String> permissionCodes
    ) {
    }

    public record SetPermissionsRequest(List<String> permissionCodes) {
    }

    public record RoleResponse(UUID id, String code, String name, UUID tenantId,
                               List<String> permissions) {
        public static RoleResponse from(Role r) {
            List<String> perms = r.getPermissions().stream()
                    .map(Permission::getCode).sorted().toList();
            return new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getTenantId(), perms);
        }
    }
}
