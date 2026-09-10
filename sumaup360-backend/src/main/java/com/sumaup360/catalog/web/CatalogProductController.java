package com.sumaup360.catalog.web;

import com.sumaup360.catalog.dto.MasterProductDtos.CatalogProductView;
import com.sumaup360.catalog.service.MasterProductService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Busqueda del catalogo maestro para tenants del SaaS: solo productos activos del rubro de
 * la empresa actual (una veterinaria no ve gaseosas). El tenant añade el producto a su
 * inventario heredando nombre/foto por referencia, sin duplicar imagenes.
 */
@RestController
@RequestMapping("/api/v1/erp/catalog/products")
@Tag(name = "Catalogo maestro (tenant)", description = "Busqueda de productos por rubro")
public class CatalogProductController {

    private final MasterProductService masterProductService;

    public CatalogProductController(MasterProductService masterProductService) {
        this.masterProductService = masterProductService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    @Operation(summary = "Busca/navega el catalogo maestro segun el rubro de la empresa (requiere product:read)")
    public List<CatalogProductView> search(@RequestParam UUID companyId,
                                           @RequestParam(required = false) String q) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return masterProductService.tenantSearch(tenantId, companyId, q);
    }
}
