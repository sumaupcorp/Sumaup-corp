package com.sumaup360.catalog.web;

import com.sumaup360.catalog.dto.MasterProductDtos.MasterProductView;
import com.sumaup360.catalog.dto.MasterProductDtos.SaveMasterProductRequest;
import com.sumaup360.catalog.service.MasterProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Administracion del catalogo maestro de productos (staff, cross-tenant). */
@RestController
@RequestMapping("/api/v1/backoffice/catalog/products")
@Tag(name = "Backoffice - Catalogo maestro", description = "Productos por rubro para el SaaS")
public class MasterProductAdminController {

    private final MasterProductService masterProductService;

    public MasterProductAdminController(MasterProductService masterProductService) {
        this.masterProductService = masterProductService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Lista/busca productos del catalogo maestro (por texto y rubro)")
    public List<MasterProductView> search(@RequestParam(required = false) String q,
                                          @RequestParam(required = false) String rubro) {
        return masterProductService.adminSearch(q, rubro);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Registra un producto en el catalogo maestro")
    public MasterProductView create(@Valid @RequestBody SaveMasterProductRequest req) {
        return masterProductService.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Edita un producto del catalogo maestro")
    public MasterProductView update(@PathVariable UUID id,
                                    @Valid @RequestBody SaveMasterProductRequest req) {
        return masterProductService.update(id, req);
    }
}
