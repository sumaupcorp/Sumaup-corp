package com.sumaup360.tenant.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.tenant.PlanLimitProvider;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Sucursales de una empresa, siempre dentro del tenant del contexto. */
@Service
public class BranchService {

    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final PlanLimitProvider planLimitProvider;

    public BranchService(BranchRepository branchRepository, CompanyRepository companyRepository,
                         PlanLimitProvider planLimitProvider) {
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.planLimitProvider = planLimitProvider;
    }

    @Transactional
    public Branch create(UUID tenantId, UUID companyId, String name, String address, boolean main) {
        Company company = requireCompanyInTenant(tenantId, companyId);
        Integer max = planLimitProvider.limits(tenantId).maxBranches();
        if (max != null && branchRepository.findByTenantId(tenantId).size() >= max) {
            throw new BadRequestException(
                    "Alcanzaste el limite de sucursales de tu plan (" + max + "). Mejora tu plan para agregar mas.");
        }
        Branch branch = new Branch();
        branch.setTenantId(tenantId);
        branch.setCompanyId(company.getId());
        branch.setName(name);
        branch.setAddress(address);
        branch.setMain(main);
        return branchRepository.save(branch);
    }

    @Transactional(readOnly = true)
    public List<Branch> listByCompany(UUID tenantId, UUID companyId) {
        requireCompanyInTenant(tenantId, companyId);
        return branchRepository.findByCompanyId(companyId);
    }

    /** Garantiza que la empresa existe y pertenece al tenant del contexto (aislamiento). */
    private Company requireCompanyInTenant(UUID tenantId, UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada."));
        if (!company.getTenantId().equals(tenantId)) {
            // No revelar existencia de recursos de otro tenant.
            throw new ResourceNotFoundException("Empresa no encontrada.");
        }
        return company;
    }
}
