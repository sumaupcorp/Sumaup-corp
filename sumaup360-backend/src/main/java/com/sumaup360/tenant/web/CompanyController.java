package com.sumaup360.tenant.web;

import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.CompanyService;
import com.sumaup360.tenant.web.dto.CompanyDtos.CompanyResponse;
import com.sumaup360.tenant.web.dto.CompanyDtos.CreateCompanyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Empresas del tenant del usuario. El tenant SIEMPRE sale del contexto (membresia),
 * nunca de un parametro del cliente: asi se garantiza el aislamiento multi-tenant.
 */
@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Empresas", description = "Empresas dentro del tenant del usuario")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('company:manage')")
    @Operation(summary = "Crea una empresa en el tenant del contexto (requiere company:manage)")
    public CompanyResponse create(@Valid @RequestBody CreateCompanyRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return CompanyResponse.from(companyService.create(
                tenantId, req.legalName(), req.ruc(), req.businessTypeCode(), req.verticalCode()));
    }

    // Contexto basico del panel: cualquier miembro autenticado del tenant puede ver a que
    // empresas pertenece (tenant-scoped por membresia). Administrarlas si exige company:manage.
    @GetMapping
    @Operation(summary = "Lista las empresas del tenant del contexto (cualquier miembro)")
    public List<CompanyResponse> list() {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return companyService.listByTenant(tenantId).stream().map(CompanyResponse::from).toList();
    }
}
