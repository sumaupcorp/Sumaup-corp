package com.sumaup360.erp.web;

import com.sumaup360.catalog.domain.MasterProduct;
import com.sumaup360.catalog.service.MasterProductProposalService;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.service.ProductService;
import com.sumaup360.erp.web.dto.ProductDtos.CreateProductRequest;
import com.sumaup360.erp.web.dto.ProductDtos.ProductResponse;
import com.sumaup360.erp.web.dto.ProductDtos.UpdateProductRequest;
import com.sumaup360.security.SecurityUtils;
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
import java.util.Map;
import java.util.UUID;

/** Productos del tenant del contexto. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Productos", description = "Catalogo de productos del negocio")
public class ProductController {

    private final ProductService productService;
    private final MasterProductProposalService proposalService;

    public ProductController(ProductService productService,
                             MasterProductProposalService proposalService) {
        this.productService = productService;
        this.proposalService = proposalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Crea un producto (requiere product:manage)")
    public ProductResponse create(@Valid @RequestBody CreateProductRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        Product created = productService.create(
                tenantId, req.companyId(), req.sku(), req.name(), req.unit(), req.price(),
                req.category(), req.masterProductId(), req.photoUrl());
        // Producto propio (sin vinculo al catalogo): se propone al catalogo maestro para
        // curacion del staff. Silencioso: nunca afecta la creacion.
        if (req.masterProductId() == null) {
            proposalService.maybePropose(tenantId, req.companyId(), created);
        }
        return toResponse(created);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('product:read')")
    @Operation(summary = "Lista productos de la empresa con foto del catalogo maestro (requiere product:read)")
    public List<ProductResponse> list(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        List<Product> items = productService.list(tenantId, companyId);
        Map<UUID, MasterProduct> masters = productService.mastersFor(items);
        return items.stream()
                .map(p -> ProductResponse.from(p,
                        p.getMasterProductId() != null ? masters.get(p.getMasterProductId()) : null))
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('product:read')")
    @Operation(summary = "Obtiene un producto (requiere product:read)")
    public ProductResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return toResponse(productService.get(id, tenantId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Actualiza un producto (requiere product:manage)")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return toResponse(productService.update(
                id, tenantId, req.name(), req.unit(), req.price(), req.category(),
                req.active(), req.photoUrl()));
    }

    private ProductResponse toResponse(Product p) {
        Map<UUID, MasterProduct> masters = productService.mastersFor(List.of(p));
        return ProductResponse.from(p,
                p.getMasterProductId() != null ? masters.get(p.getMasterProductId()) : null);
    }
}
