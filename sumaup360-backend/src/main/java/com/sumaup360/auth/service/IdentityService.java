package com.sumaup360.auth.service;

import com.sumaup360.auth.domain.AccountStatus;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.Role;
import com.sumaup360.auth.domain.UserType;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.auth.repository.RoleRepository;
import com.sumaup360.security.AppUserPrincipal;
import com.sumaup360.security.SecurityProperties;
import com.sumaup360.security.TenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resuelve la identidad efectiva del backend a partir de una identidad de Firebase ya
 * verificada, provisionando al usuario la primera vez. Construye el principal con sus
 * permisos. El backend es la autoridad: aqui se decide "que puede hacer".
 */
@Service
public class IdentityService {

    private static final Logger log = LoggerFactory.getLogger(IdentityService.class);

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SecurityProperties securityProperties;
    private final TenantResolver tenantResolver;

    public IdentityService(AppUserRepository userRepository,
                           RoleRepository roleRepository,
                           SecurityProperties securityProperties,
                           TenantResolver tenantResolver) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.securityProperties = securityProperties;
        this.tenantResolver = tenantResolver;
    }

    /**
     * Devuelve el principal autenticado para un firebaseUid verificado, creando el usuario
     * si no existe. Tenant aun no se resuelve en Fase 1 (sin membresia persistida).
     */
    @Transactional
    public AppUserPrincipal authenticate(String firebaseUid, String email, String displayName) {
        AppUser user = userRepository.findWithRolesByFirebaseUid(firebaseUid)
                .orElseGet(() -> provision(firebaseUid, email, displayName));

        // Garantiza el admin de bootstrap aunque el usuario ya existiera (idempotente).
        ensureBootstrapAdmin(user);

        UUID tenantId = tenantResolver.resolveDefaultTenant(user.getId());

        // Solo aplican los roles globales/staff (tenant_id NULL) y los del tenant ACTIVO.
        // Asi un usuario en varios tenants no arrastra permisos de otro tenant.
        List<Role> effectiveRoles = user.getRoles().stream()
                .filter(r -> r.getTenantId() == null
                        || (tenantId != null && tenantId.equals(r.getTenantId())))
                .toList();

        Set<String> roleCodes = effectiveRoles.stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        Set<String> permissionCodes = effectiveRoles.stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getCode())
                .collect(Collectors.toSet());

        return new AppUserPrincipal(
                user.getId(),
                user.getFirebaseUid(),
                user.getEmail(),
                user.getUserType(),
                tenantId,
                permissionCodes,
                roleCodes
        );
    }

    private AppUser provision(String firebaseUid, String email, String displayName) {
        AppUser user = new AppUser();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setUserType(UserType.PERSON);
        user.setStatus(AccountStatus.PENDING);
        AppUser saved = userRepository.save(user);
        log.info("Usuario provisionado: {} (tipo {}).", firebaseUid, saved.getUserType());
        return saved;
    }

    /**
     * Si el uid coincide con security.bootstrap-admin-uid y aun no es admin staff, le asigna
     * el rol global 'admin' y lo marca STAFF/ACTIVE. Idempotente: solo escribe si falta.
     * Pensado para crear el primer super-admin del sistema desde cero.
     */
    private void ensureBootstrapAdmin(AppUser user) {
        String bootstrapUid = securityProperties.getBootstrapAdminUid();
        if (bootstrapUid == null || bootstrapUid.isBlank()
                || !bootstrapUid.equals(user.getFirebaseUid())) {
            return;
        }
        boolean alreadyAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getTenantId() == null && "admin".equals(r.getCode()));
        if (alreadyAdmin) {
            return;
        }
        roleRepository.findByCodeAndTenantIdIsNull("admin").ifPresent(admin -> {
            user.getRoles().add(admin);
            user.setUserType(UserType.STAFF);
            user.setStatus(AccountStatus.ACTIVE);
            userRepository.save(user);
            log.warn("Bootstrap: uid {} promovido a STAFF admin.", user.getFirebaseUid());
        });
    }
}
