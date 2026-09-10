package com.sumaup360.backoffice.service;

import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.repository.ReceiptRepository;
import com.sumaup360.auth.domain.UserType;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.backoffice.dto.OverviewDtos.Overview;
import com.sumaup360.backoffice.dto.OverviewDtos.PlanCount;
import com.sumaup360.billing.domain.Plan;
import com.sumaup360.billing.domain.Subscription;
import com.sumaup360.billing.enums.SubscriptionStatus;
import com.sumaup360.billing.repository.PlanRepository;
import com.sumaup360.billing.repository.SubscriptionRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import com.sumaup360.tenant.repository.MembershipRepository;
import com.sumaup360.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Estadisticas agregadas del ecosistema para el dashboard del Backoffice. */
@Service
public class StatsService {

    private final TenantRepository tenantRepository;
    private final CompanyRepository companyRepository;
    private final BranchRepository branchRepository;
    private final MembershipRepository membershipRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final ReceiptRepository receiptRepository;
    private final AppUserRepository userRepository;

    public StatsService(TenantRepository tenantRepository, CompanyRepository companyRepository,
                        BranchRepository branchRepository, MembershipRepository membershipRepository,
                        SubscriptionRepository subscriptionRepository, PlanRepository planRepository,
                        ReceiptRepository receiptRepository, AppUserRepository userRepository) {
        this.tenantRepository = tenantRepository;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.membershipRepository = membershipRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.receiptRepository = receiptRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Overview overview() {
        Map<UUID, String> planCodeById = planRepository.findAll().stream()
                .collect(Collectors.toMap(Plan::getId, Plan::getCode, (a, b) -> a));

        List<Subscription> active = subscriptionRepository.findAll().stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .toList();

        List<PlanCount> byPlan = active.stream()
                .collect(Collectors.groupingBy(
                        s -> planCodeById.getOrDefault(s.getPlanId(), "desconocido"),
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> new PlanCount(e.getKey(), e.getValue()))
                .sorted((a, b) -> Long.compare(b.count(), a.count()))
                .toList();

        long staff = userRepository.findByUserType(UserType.STAFF).size();

        return new Overview(
                tenantRepository.count(),
                companyRepository.count(),
                branchRepository.count(),
                membershipRepository.count(),
                staff,
                receiptRepository.countByStatus(ReceiptStatus.PENDING),
                receiptRepository.countByStatus(ReceiptStatus.IN_PROCESS),
                receiptRepository.countByStatus(ReceiptStatus.PROCESSED),
                active.size(),
                byPlan);
    }
}
