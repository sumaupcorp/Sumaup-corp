package com.sumaup360.tenant.web;

import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.BranchService;
import com.sumaup360.tenant.web.dto.BranchDtos.BranchResponse;
import com.sumaup360.tenant.web.dto.BranchDtos.CreateBranchRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Sucursales de una empresa, dentro del tenant del contexto. */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/branches")
@Tag(name = "Sucursales", description = "Sucursales de una empresa")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('branch:manage')")
    @Operation(summary = "Crea una sucursal (requiere branch:manage)")
    public BranchResponse create(@PathVariable UUID companyId,
                                 @Valid @RequestBody CreateBranchRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        boolean main = req.main() != null && req.main();
        return BranchResponse.from(
                branchService.create(tenantId, companyId, req.name(), req.address(), main));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('branch:read')")
    @Operation(summary = "Lista las sucursales de una empresa (requiere branch:read)")
    public List<BranchResponse> list(@PathVariable UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return branchService.listByCompany(tenantId, companyId).stream()
                .map(BranchResponse::from).toList();
    }
}
