package com.sumaup360.erp.service;

import com.sumaup360.catalog.domain.MasterProduct;
import com.sumaup360.catalog.repository.MasterProductRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Productos del catalogo del tenant. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final MasterProductRepository masterProductRepository;
    private final CompanyRepository companyRepository;

    public ProductService(ProductRepository productRepository,
                          MasterProductRepository masterProductRepository,
                          CompanyRepository companyRepository) {
        this.productRepository = productRepository;
        this.masterProductRepository = masterProductRepository;
        this.companyRepository = companyRepository;
    }

    /** Toda operacion de catalogo exige una empresa valida del tenant (aislamiento). */
    private void requireCompany(UUID tenantId, UUID companyId) {
        if (companyId == null) {
            throw new BadRequestException("Falta la empresa (companyId).");
        }
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }

    /** Productos del catalogo maestro referenciados por estos productos (para heredar foto). */
    @Transactional(readOnly = true)
    public Map<UUID, MasterProduct> mastersFor(Collection<Product> items) {
        List<UUID> ids = items.stream().map(Product::getMasterProductId)
                .filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return masterProductRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MasterProduct::getId, Function.identity()));
    }

    @Transactional
    public Product create(UUID tenantId, UUID companyId, String sku, String name, String unit,
                          BigDecimal price, String category, UUID masterProductId, String photoUrl) {
        requireCompany(tenantId, companyId);
        if (productRepository.existsByTenantIdAndCompanyIdAndSku(tenantId, companyId, sku)) {
            throw new ConflictException("Ya existe un producto con SKU '" + sku + "' en esta empresa.");
        }
        Product p = new Product();
        p.setTenantId(tenantId);
        p.setCompanyId(companyId);
        p.setSku(sku);
        p.setName(name);
        if (unit != null && !unit.isBlank()) {
            p.setUnit(unit);
        }
        p.setPrice(price != null ? price : BigDecimal.ZERO);
        p.setCategory(category);
        p.setActive(true);
        p.setMasterProductId(masterProductId);
        p.setPhotoUrl(photoUrl != null && !photoUrl.isBlank() ? photoUrl : null);
        return productRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<Product> list(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        return productRepository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Transactional(readOnly = true)
    public Product get(UUID id, UUID tenantId) {
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
    }

    @Transactional
    public Product update(UUID id, UUID tenantId, String name, String unit,
                          BigDecimal price, String category, Boolean active, String photoUrl) {
        Product p = get(id, tenantId);
        if (name != null) p.setName(name);
        if (unit != null && !unit.isBlank()) p.setUnit(unit);
        if (price != null) p.setPrice(price);
        if (category != null) p.setCategory(category);
        if (active != null) p.setActive(active);
        // Cadena vacia = quitar la foto propia (vuelve a heredar la del maestro); null = no tocar.
        if (photoUrl != null) p.setPhotoUrl(photoUrl.isBlank() ? null : photoUrl);
        return productRepository.save(p);
    }
}
