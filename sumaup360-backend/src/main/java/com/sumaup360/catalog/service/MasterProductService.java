package com.sumaup360.catalog.service;

import com.sumaup360.catalog.domain.MasterProduct;
import com.sumaup360.catalog.dto.MasterProductDtos.CatalogProductView;
import com.sumaup360.catalog.dto.MasterProductDtos.MasterProductView;
import com.sumaup360.catalog.dto.MasterProductDtos.SaveMasterProductRequest;
import com.sumaup360.catalog.repository.BusinessTypeRepository;
import com.sumaup360.catalog.repository.MasterProductRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Catalogo maestro de productos: administracion staff + busqueda del tenant por rubro. */
@Service
public class MasterProductService {

    private static final int MAX_RESULTS = 50;

    private final MasterProductRepository masterProductRepository;
    private final BusinessTypeRepository businessTypeRepository;
    private final CompanyRepository companyRepository;

    public MasterProductService(MasterProductRepository masterProductRepository,
                                BusinessTypeRepository businessTypeRepository,
                                CompanyRepository companyRepository) {
        this.masterProductRepository = masterProductRepository;
        this.businessTypeRepository = businessTypeRepository;
        this.companyRepository = companyRepository;
    }

    // ---- Staff (backoffice) ----

    @Transactional(readOnly = true)
    public List<MasterProductView> adminSearch(String q, String rubro) {
        String query = normalize(q);
        return masterProductRepository
                .adminSearch(likePattern(query), query, normalize(rubro), PageRequest.of(0, MAX_RESULTS))
                .stream().map(MasterProductView::from).toList();
    }

    @Transactional
    public MasterProductView create(SaveMasterProductRequest req) {
        MasterProduct p = new MasterProduct();
        apply(p, req);
        return MasterProductView.from(masterProductRepository.save(p));
    }

    @Transactional
    public MasterProductView update(UUID id, SaveMasterProductRequest req) {
        MasterProduct p = masterProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado en el catalogo."));
        apply(p, req);
        return MasterProductView.from(masterProductRepository.save(p));
    }

    private void apply(MasterProduct p, SaveMasterProductRequest req) {
        String ean = normalize(req.ean());
        if (ean != null) {
            masterProductRepository.findByEan(ean).ifPresent(existing -> {
                if (!existing.getId().equals(p.getId())) {
                    throw new BadRequestException(
                            "Ya existe un producto con ese codigo de barras: " + existing.getName());
                }
            });
        }
        Set<String> rubros = req.rubros();
        if (rubros == null || rubros.isEmpty()) {
            throw new BadRequestException("Elige al menos un rubro para el producto.");
        }
        for (String rubro : rubros) {
            if (businessTypeRepository.findByCode(rubro).isEmpty()) {
                throw new BadRequestException("Rubro desconocido: " + rubro);
            }
        }
        p.setEan(ean);
        p.setName(req.name().trim());
        p.setBrand(trimOrNull(req.brand()));
        p.setCategory(trimOrNull(req.category()));
        p.setPresentation(trimOrNull(req.presentation()));
        p.setPhotoUrl(trimOrNull(req.photoUrl()));
        String external = trimOrNull(req.photoExternalUrl());
        if (external != null && !external.startsWith("https://")) {
            throw new BadRequestException("La URL externa de la foto debe empezar con https://");
        }
        p.setPhotoExternalUrl(external);
        p.setPhotoSource(trimOrNull(req.photoSource()));
        p.setPhotoSourceUrl(trimOrNull(req.photoSourceUrl()));
        if (req.verified() != null) p.setVerified(req.verified());
        if (req.active() != null) p.setActive(req.active());
        p.getRubros().clear();
        p.getRubros().addAll(rubros);
    }

    // ---- Tenant (SaaS): busca solo lo de su rubro ----

    @Transactional(readOnly = true)
    public List<CatalogProductView> tenantSearch(UUID tenantId, UUID companyId, String q) {
        String query = normalize(q);
        Company company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
        if (company.getBusinessTypeCode() == null) {
            return List.of();
        }
        return masterProductRepository
                .tenantSearch(likePattern(query), query, company.getBusinessTypeCode(),
                        PageRequest.of(0, MAX_RESULTS))
                .stream().map(CatalogProductView::from).toList();
    }

    /**
     * Tokeniza la busqueda: "coca cola" -> "%coca%cola%". Asi las palabras coinciden aunque
     * el texto del catalogo tenga guiones u otras palabras en medio ("Gaseosa Coca-Cola 500ml").
     */
    private static String likePattern(String query) {
        if (query == null) {
            return null;
        }
        return "%" + query.toLowerCase().replaceAll("\\s+", "%") + "%";
    }

    private static String normalize(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
