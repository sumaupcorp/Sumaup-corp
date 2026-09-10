package com.sumaup360.tenant.service;

import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Empresas dentro de un tenant. Todas las operaciones reciben el tenantId del contexto
 * (resuelto desde la membresia del usuario); nunca se confia en un tenant del cliente.
 */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Company create(UUID tenantId, String legalName, String ruc,
                          String businessTypeCode, String verticalCode) {
        Company company = new Company();
        company.setTenantId(tenantId);
        company.setLegalName(legalName);
        company.setRuc(ruc);
        company.setBusinessTypeCode(businessTypeCode);
        company.setVerticalCode(verticalCode);
        return companyRepository.save(company);
    }

    @Transactional(readOnly = true)
    public List<Company> listByTenant(UUID tenantId) {
        return companyRepository.findByTenantId(tenantId);
    }
}
