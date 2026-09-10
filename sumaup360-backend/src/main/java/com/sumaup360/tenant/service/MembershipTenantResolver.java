package com.sumaup360.tenant.service;

import com.sumaup360.security.TenantResolver;
import com.sumaup360.tenant.domain.Membership;
import com.sumaup360.tenant.repository.MembershipRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Resuelve el tenant por defecto del usuario a partir de su membresia. */
@Component
public class MembershipTenantResolver implements TenantResolver {

    private final MembershipRepository membershipRepository;

    public MembershipTenantResolver(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public UUID resolveDefaultTenant(UUID userId) {
        return membershipRepository.findByUserIdAndDefaultTenantTrue(userId)
                .map(Membership::getTenantId)
                .orElse(null);
    }
}
