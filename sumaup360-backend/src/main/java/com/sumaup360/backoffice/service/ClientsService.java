package com.sumaup360.backoffice.service;

import com.sumaup360.backoffice.dto.ClientsDtos.ClientView;
import com.sumaup360.billing.domain.Plan;
import com.sumaup360.billing.enums.SubscriptionStatus;
import com.sumaup360.billing.repository.PlanRepository;
import com.sumaup360.billing.repository.SubscriptionRepository;
import com.sumaup360.tenant.domain.Tenant;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import com.sumaup360.tenant.repository.MembershipRepository;
import com.sumaup360.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Panorama de clientes (tenants) del SaaS para el Backoffice. Cross-tenant. */
@Service
public class ClientsService {

    private final TenantRepository tenantRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public ClientsService(TenantRepository tenantRepository,
                          CompanyRepository companyRepository,
                          BranchRepository branchRepository,
                          MembershipRepository membershipRepository,
                          SubscriptionRepository subscriptionRepository,
                          PlanRepository planRepository) {
        this.tenantRepository = tenantRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    @Transactional(readOnly = true)
    public List<ClientView> list() {
        return tenantRepository.findAll().stream().map(this::toView).toList();
    }

    private ClientView toView(Tenant t) {
        String plan = subscriptionRepository
                .findFirstByTenantIdAndStatus(t.getId(), SubscriptionStatus.ACTIVE)
                .flatMap(s -> planRepository.findById(s.getPlanId()))
                .map(Plan::getCode)
                .orElse(null);
        return new ClientView(
                t.getId(), t.getCode(), t.getName(), t.getStatus(),
                companyRepository.findByTenantId(t.getId()).size(),
                branchRepository.findByTenantId(t.getId()).size(),
                membershipRepository.findByTenantId(t.getId()).size(),
                plan);
    }
}
