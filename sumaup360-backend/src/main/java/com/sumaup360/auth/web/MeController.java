package com.sumaup360.auth.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.auth.web.dto.MeResponse;
import com.sumaup360.security.AppUserPrincipal;
import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.domain.Membership;
import com.sumaup360.tenant.repository.MembershipRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Identidad del usuario autenticado. /me es la base que consumen app, SaaS y backoffice
 * para saber su contexto, roles, permisos y asignacion operativa (sede + modulos).
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Identidad", description = "Usuario autenticado y su contexto")
public class MeController {

    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;

    public MeController(MembershipRepository membershipRepository, ObjectMapper objectMapper) {
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "Devuelve el usuario autenticado, sus roles, permisos y asignacion")
    public MeResponse me() {
        AppUserPrincipal p = SecurityUtils.currentPrincipal();
        Membership m = p.tenantId() != null
                ? membershipRepository.findByUserIdAndTenantId(p.userId(), p.tenantId()).orElse(null)
                : null;
        return new MeResponse(p.userId(), p.firebaseUid(), p.email(), p.userType(),
                p.tenantId(), p.roles(), p.permissions(),
                m != null ? m.getBranchId() : null,
                parseModules(m != null ? m.getAllowedModules() : null));
    }

    /**
     * Endpoint de ejemplo protegido por permiso, para verificar el RBAC end-to-end.
     * Requiere el permiso 'staff:read' (lo tienen admin, gerencia, desarrollador).
     */
    @GetMapping("/permissions")
    @Operation(summary = "Lista los permisos del usuario (requiere staff:read)")
    @PreAuthorize("hasAuthority('staff:read')")
    public Set<String> myPermissions() {
        return SecurityUtils.currentPrincipal().permissions();
    }

    private List<String> parseModules(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception e) {
            return null;
        }
    }
}
