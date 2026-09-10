package com.sumaup360.tenant.web.dto;

import com.sumaup360.auth.domain.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** DTOs de gestion de usuarios (trabajadores) dentro de un tenant. */
public final class TenantUserDtos {

    private TenantUserDtos() {
    }

    /**
     * Usuario del tenant con sus roles propios de ese tenant, su sede asignada
     * (null = todas), el negocio al que pertenece esa sede (null = acceso a todo
     * el tenant) y los modulos que ve (null = todos los habilitados).
     */
    public record TenantUserResponse(
            UUID userId,
            String firebaseUid,
            String email,
            String displayName,
            UserType userType,
            List<String> roles,
            UUID branchId,
            String branchName,
            UUID companyId,
            String companyName,
            List<String> allowedModules
    ) {
    }

    public record AssignRoleRequest(@NotNull UUID roleId) {
    }

    /** Alta de un trabajador: cuenta Firebase + membresia + rol/sede/modulos opcionales. */
    public record CreateWorkerRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotBlank @Size(max = 120) String name,
            UUID roleId,
            UUID branchId,
            List<String> allowedModules
    ) {
    }

    /** Asignacion operativa (PUT completo): sede (null = todas) y modulos (null = todos). */
    public record UpdateAssignmentRequest(
            UUID branchId,
            List<String> allowedModules
    ) {
    }
}
