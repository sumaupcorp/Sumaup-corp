package com.sumaup360.erp.module.web;

import com.sumaup360.erp.module.dto.ModuleDtos.CompanyModuleView;
import com.sumaup360.erp.module.dto.ModuleDtos.SetModuleRequest;
import com.sumaup360.erp.module.service.ModuleEnablementService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Modulos habilitados por empresa (Fase 6). El menu/navegacion del SaaS se construye desde
 * /enabled. El backend es la autoridad: aqui se resuelve que modulos tiene cada empresa.
 */
@RestController
@RequestMapping("/api/v1/erp/companies/{companyId}/modules")
@Tag(name = "Modulos por empresa", description = "Habilitacion de modulos por rubro y override")
public class CompanyModuleController {

    private final ModuleEnablementService moduleService;

    public CompanyModuleController(ModuleEnablementService moduleService) {
        this.moduleService = moduleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('company:read')")
    @Operation(summary = "Lista los modulos de la empresa con su estado (requiere company:read)")
    public List<CompanyModuleView> list(@PathVariable UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return moduleService.list(tenantId, companyId);
    }

    // Contexto basico: el menu del SaaS se arma con esto, asi que cualquier miembro
    // autenticado del tenant debe poder leerlo (tenant-scoped). Editar exige company:manage.
    @GetMapping("/enabled")
    @Operation(summary = "Codigos de modulos habilitados (lo que muestra la UI) (cualquier miembro)")
    public List<String> enabled(@PathVariable UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return moduleService.enabledCodes(tenantId, companyId);
    }

    @PostMapping("/provision")
    @PreAuthorize("hasAuthority('company:manage')")
    @Operation(summary = "Provisiona los modulos por defecto del rubro (requiere company:manage)")
    public List<CompanyModuleView> provision(@PathVariable UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return moduleService.provision(tenantId, companyId);
    }

    @PutMapping("/{moduleCode}")
    @PreAuthorize("hasAuthority('company:manage')")
    @Operation(summary = "Habilita/deshabilita un modulo de la empresa (requiere company:manage)")
    public CompanyModuleView setModule(@PathVariable UUID companyId,
                                       @PathVariable String moduleCode,
                                       @Valid @RequestBody SetModuleRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return moduleService.setModule(tenantId, companyId, moduleCode, req.enabled());
    }
}
