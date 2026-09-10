package com.sumaup360.erp.web;

import com.sumaup360.erp.service.CustomerService;
import com.sumaup360.erp.web.dto.CustomerDtos.CreateCustomerRequest;
import com.sumaup360.erp.web.dto.CustomerDtos.CustomerResponse;
import com.sumaup360.security.SecurityUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Clientes del tenant del contexto. */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Clientes", description = "Clientes del negocio")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('customer:manage')")
    @Operation(summary = "Crea un cliente (requiere customer:manage)")
    public CustomerResponse create(@Valid @RequestBody CreateCustomerRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return CustomerResponse.from(customerService.create(
                tenantId, req.companyId(), req.name(), req.docType(), req.docNumber(),
                req.email(), req.phone()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Lista clientes de la empresa (requiere customer:read)")
    public List<CustomerResponse> list(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return customerService.list(tenantId, companyId).stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Obtiene un cliente (requiere customer:read)")
    public CustomerResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return CustomerResponse.from(customerService.get(id, tenantId));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Ficha del cliente: estadisticas de atencion, compras, mascotas e historial (requiere customer:read)")
    public com.sumaup360.erp.web.dto.CustomerDtos.CustomerSummaryResponse summary(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return customerService.summary(id, tenantId);
    }
}
