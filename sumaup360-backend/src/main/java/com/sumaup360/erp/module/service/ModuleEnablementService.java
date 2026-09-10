package com.sumaup360.erp.module.service;

import com.sumaup360.catalog.domain.ModuleCatalog;
import com.sumaup360.catalog.repository.BusinessTypeModuleRepository;
import com.sumaup360.catalog.repository.ModuleCatalogRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.module.LicenseModuleProvider;
import com.sumaup360.erp.module.domain.CompanyModule;
import com.sumaup360.erp.module.dto.ModuleDtos.CompanyModuleView;
import com.sumaup360.erp.module.repository.CompanyModuleRepository;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Motor de modulos habilitados por empresa:
 *   defaults(rubro)  +/-  overrides(empresa)  =  enabledModules
 * (El techo por plan se aplicara cuando exista billing. TODO Fase 8.)
 * Una sola base de codigo: los rubros se configuran, no se duplican.
 */
@Service
public class ModuleEnablementService {

    private final CompanyRepository companyRepository;
    private final BusinessTypeModuleRepository businessTypeModuleRepository;
    private final CompanyModuleRepository companyModuleRepository;
    private final ModuleCatalogRepository moduleCatalogRepository;
    private final LicenseModuleProvider licenseModuleProvider;

    public ModuleEnablementService(CompanyRepository companyRepository,
                                   BusinessTypeModuleRepository businessTypeModuleRepository,
                                   CompanyModuleRepository companyModuleRepository,
                                   ModuleCatalogRepository moduleCatalogRepository,
                                   LicenseModuleProvider licenseModuleProvider) {
        this.companyRepository = companyRepository;
        this.businessTypeModuleRepository = businessTypeModuleRepository;
        this.companyModuleRepository = companyModuleRepository;
        this.moduleCatalogRepository = moduleCatalogRepository;
        this.licenseModuleProvider = licenseModuleProvider;
    }

    /** Crea los modulos por defecto del rubro para la empresa (idempotente). */
    @Transactional
    public List<CompanyModuleView> provision(UUID tenantId, UUID companyId) {
        Company company = requireCompany(tenantId, companyId);
        Set<String> existing = companyModuleRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                .stream().map(CompanyModule::getModuleCode).collect(Collectors.toSet());
        for (String code : defaultModuleCodes(company)) {
            if (!existing.contains(code)) {
                CompanyModule cm = new CompanyModule();
                cm.setTenantId(tenantId);
                cm.setCompanyId(companyId);
                cm.setModuleCode(code);
                cm.setEnabled(true);
                cm.setSource("DEFAULT");
                companyModuleRepository.save(cm);
            }
        }
        return list(tenantId, companyId);
    }

    /** Codigos de los modulos habilitados (lo que la UI muestra y el backend permite). */
    @Transactional
    public List<String> enabledCodes(UUID tenantId, UUID companyId) {
        ensureProvisioned(tenantId, companyId);
        Set<String> allowed = licenseModuleProvider.allowedModules(tenantId).orElse(null);
        return companyModuleRepository.findByTenantIdAndCompanyId(tenantId, companyId).stream()
                .filter(CompanyModule::isEnabled)
                .map(CompanyModule::getModuleCode)
                .filter(code -> allowed == null || allowed.contains(code))  // techo por plan
                .sorted()
                .toList();
    }

    @Transactional
    public List<CompanyModuleView> list(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        ensureProvisioned(tenantId, companyId);
        Set<String> allowed = licenseModuleProvider.allowedModules(tenantId).orElse(null);
        Map<String, ModuleCatalog> catalog = moduleCatalogRepository.findAll().stream()
                .collect(Collectors.toMap(ModuleCatalog::getCode, Function.identity(), (a, b) -> a));
        List<CompanyModule> rows = companyModuleRepository.findByTenantIdAndCompanyId(tenantId, companyId);
        Set<String> present = rows.stream().map(CompanyModule::getModuleCode).collect(Collectors.toSet());

        java.util.ArrayList<CompanyModuleView> result = new java.util.ArrayList<>();
        for (CompanyModule cm : rows) {
            ModuleCatalog m = catalog.get(cm.getModuleCode());
            // enabled efectivo = habilitado por la empresa Y permitido por el plan (si hay).
            boolean effective = cm.isEnabled()
                    && (allowed == null || allowed.contains(cm.getModuleCode()));
            result.add(new CompanyModuleView(cm.getModuleCode(),
                    m != null ? m.getName() : cm.getModuleCode(),
                    effective, cm.getSource(), m != null && m.isCore()));
        }
        // Modulos del catalogo que el plan permite pero la empresa nunca toco (p. ej.
        // facturacion electronica, sin rubro por defecto): se listan apagados para que
        // el negocio pueda descubrirlos y activarlos cuando los necesite.
        for (ModuleCatalog m : catalog.values()) {
            if (m.isActive() && !present.contains(m.getCode())
                    && (allowed == null || allowed.contains(m.getCode()))) {
                result.add(new CompanyModuleView(m.getCode(), m.getName(), false, "AVAILABLE", m.isCore()));
            }
        }
        result.sort((a, b) -> a.moduleCode().compareTo(b.moduleCode()));
        return result;
    }

    /** Habilita/deshabilita un modulo para la empresa (override del administrador). */
    @Transactional
    public CompanyModuleView setModule(UUID tenantId, UUID companyId, String moduleCode, boolean enabled) {
        requireCompany(tenantId, companyId);
        ModuleCatalog module = moduleCatalogRepository.findByCode(moduleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Modulo no existe en el catalogo."));
        CompanyModule cm = companyModuleRepository
                .findByTenantIdAndCompanyIdAndModuleCode(tenantId, companyId, moduleCode)
                .orElseGet(() -> {
                    CompanyModule n = new CompanyModule();
                    n.setTenantId(tenantId);
                    n.setCompanyId(companyId);
                    n.setModuleCode(moduleCode);
                    return n;
                });
        cm.setEnabled(enabled);
        cm.setSource("OVERRIDE");
        companyModuleRepository.save(cm);
        return new CompanyModuleView(moduleCode, module.getName(), enabled, "OVERRIDE", module.isCore());
    }

    // ---- helpers ----

    private void ensureProvisioned(UUID tenantId, UUID companyId) {
        if (companyModuleRepository.findByTenantIdAndCompanyId(tenantId, companyId).isEmpty()) {
            Company company = requireCompany(tenantId, companyId);
            for (String code : defaultModuleCodes(company)) {
                CompanyModule cm = new CompanyModule();
                cm.setTenantId(tenantId);
                cm.setCompanyId(companyId);
                cm.setModuleCode(code);
                cm.setEnabled(true);
                cm.setSource("DEFAULT");
                companyModuleRepository.save(cm);
            }
        }
    }

    private List<String> defaultModuleCodes(Company company) {
        if (company.getBusinessTypeCode() != null && !company.getBusinessTypeCode().isBlank()) {
            List<String> fromRubro = businessTypeModuleRepository
                    .findByBusinessTypeCode(company.getBusinessTypeCode()).stream()
                    .map(bm -> bm.getModuleCode()).toList();
            if (!fromRubro.isEmpty()) {
                return fromRubro;
            }
        }
        // Sin rubro (o rubro sin config): se habilitan los modulos core.
        return moduleCatalogRepository.findByActiveTrue().stream()
                .filter(ModuleCatalog::isCore).map(ModuleCatalog::getCode).toList();
    }

    private Company requireCompany(UUID tenantId, UUID companyId) {
        return companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }
}
