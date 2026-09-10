package com.sumaup360.tenant.service;

import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.Role;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.auth.repository.RoleRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.tenant.PlanLimitProvider;
import com.sumaup360.tenant.domain.Membership;
import com.sumaup360.tenant.repository.MembershipRepository;
import com.sumaup360.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Vincula usuarios a tenants. Asigna el tenant por defecto del usuario. */
@Service
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PlanLimitProvider planLimitProvider;

    public MembershipService(MembershipRepository membershipRepository,
                             TenantRepository tenantRepository,
                             AppUserRepository userRepository,
                             RoleRepository roleRepository,
                             PlanLimitProvider planLimitProvider) {
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.planLimitProvider = planLimitProvider;
    }

    @Transactional
    public Membership addMember(UUID tenantId, String firebaseUid, boolean asDefault, boolean asAdmin) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado."));

        AppUser user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado. Debe iniciar sesion al menos una vez."));

        if (membershipRepository.existsByUserIdAndTenantId(user.getId(), tenantId)) {
            throw new ConflictException("El usuario ya pertenece a este tenant.");
        }

        Integer maxUsers = planLimitProvider.limits(tenantId).maxUsers();
        if (maxUsers != null && membershipRepository.findByTenantId(tenantId).size() >= maxUsers) {
            throw new BadRequestException(
                    "Alcanzaste el limite de usuarios de tu plan (" + maxUsers + "). Mejora tu plan para agregar mas.");
        }

        if (asDefault) {
            // Solo un tenant por defecto por usuario.
            membershipRepository.findByUserId(user.getId()).forEach(m -> {
                if (m.isDefaultTenant()) {
                    m.setDefaultTenant(false);
                    membershipRepository.save(m);
                }
            });
        }

        if (asAdmin) {
            Role admin = roleRepository.findByCodeAndTenantId("tenant-admin", tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "El tenant no tiene rol administrador configurado."));
            user.getRoles().add(admin);
            userRepository.save(user);
        }

        Membership membership = new Membership();
        membership.setUserId(user.getId());
        membership.setTenantId(tenantId);
        membership.setDefaultTenant(asDefault);
        membership.setStatus("ACTIVE");
        return membershipRepository.save(membership);
    }
}
