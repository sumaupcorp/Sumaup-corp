package com.sumaup360.tenant.service;

import com.sumaup360.common.error.ApiException;
import com.sumaup360.tenant.repository.MembershipRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enforcement de la sede asignada: si la membresia del usuario tiene branch_id, solo puede
 * operar (vender, abrir caja) en ESA sucursal. Sin asignacion (dueño/admin) opera en todas.
 * La lectura/reportes no se restringe: el analisis consolidado es para el dueño.
 */
@Service
public class BranchAccessService {

    private final MembershipRepository membershipRepository;

    public BranchAccessService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Transactional(readOnly = true)
    public void assertCanOperate(UUID tenantId, UUID userId, UUID branchId) {
        membershipRepository.findByUserIdAndTenantId(userId, tenantId).ifPresent(m -> {
            if (m.getBranchId() != null && !m.getBranchId().equals(branchId)) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Tu cuenta esta asignada a otra sede: no puedes operar en esta sucursal.");
            }
        });
    }
}
