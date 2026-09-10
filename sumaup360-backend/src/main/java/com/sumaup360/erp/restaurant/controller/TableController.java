package com.sumaup360.erp.restaurant.controller;

import com.sumaup360.erp.restaurant.dto.TableDtos.CreateTableRequest;
import com.sumaup360.erp.restaurant.dto.TableDtos.TableResponse;
import com.sumaup360.erp.restaurant.dto.TableDtos.UpdateTableStatusRequest;
import com.sumaup360.erp.restaurant.service.TableService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Mesas del restaurante (vertical). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp/restaurant/tables")
@Tag(name = "Restaurante - Mesas", description = "Gestion de mesas por sucursal")
public class TableController {

    private final TableService tableService;

    public TableController(TableService tableService) {
        this.tableService = tableService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('table:manage')")
    @Operation(summary = "Crea una mesa (requiere table:manage)")
    public TableResponse create(@Valid @RequestBody CreateTableRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return TableResponse.from(
                tableService.create(tenantId, req.branchId(), req.name(), req.zone(), req.capacity()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('table:read')")
    @Operation(summary = "Lista mesas de una sucursal (requiere table:read)")
    public List<TableResponse> list(@RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return tableService.list(tenantId, branchId).stream().map(TableResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('table:read')")
    @Operation(summary = "Detalle de mesa (requiere table:read)")
    public TableResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return TableResponse.from(tableService.get(id, tenantId));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('table:manage')")
    @Operation(summary = "Cambia el estado de una mesa (requiere table:manage)")
    public TableResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateTableStatusRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return TableResponse.from(tableService.updateStatus(id, tenantId, req.status()));
    }
}
