package com.sumaup360.catalog.service;

import com.sumaup360.catalog.domain.MasterProduct;
import com.sumaup360.catalog.domain.MasterProductProposal;
import com.sumaup360.catalog.repository.MasterProductProposalRepository;
import com.sumaup360.catalog.repository.MasterProductRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Propuestas de los tenants al catalogo maestro. La creacion es silenciosa (no molesta al
 * tenant); la curacion es del staff: aprobar crea el master_product y vincula el producto
 * original del tenant (inyectamos ProductRepository del erp por eso — monolito modular).
 */
@Service
public class MasterProductProposalService {

    private static final Logger log = LoggerFactory.getLogger(MasterProductProposalService.class);

    private final MasterProductProposalRepository proposalRepository;
    private final MasterProductRepository masterProductRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;

    public MasterProductProposalService(MasterProductProposalRepository proposalRepository,
                                        MasterProductRepository masterProductRepository,
                                        CompanyRepository companyRepository,
                                        ProductRepository productRepository) {
        this.proposalRepository = proposalRepository;
        this.masterProductRepository = masterProductRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
    }

    /**
     * Registra una propuesta a partir de un producto propio del tenant. Silenciosa y
     * best-effort: si no hay rubro, ya existe en el catalogo o ya hay una propuesta
     * pendiente igual, simplemente no propone (nunca falla la creacion del producto).
     */
    @Transactional
    public void maybePropose(UUID tenantId, UUID companyId, Product product) {
        try {
            String businessType = companyRepository.findByIdAndTenantId(companyId, tenantId)
                    .map(c -> c.getBusinessTypeCode())
                    .orElse(null);
            if (businessType == null) {
                return;
            }
            String ean = product.getSku() != null && product.getSku().matches("\\d{8,14}")
                    ? product.getSku() : null;
            if (ean != null && masterProductRepository.findByEan(ean).isPresent()) {
                return; // ya existe en el catalogo con ese codigo
            }
            if (proposalRepository.existsByTenantIdAndNameIgnoreCaseAndStatus(
                    tenantId, product.getName(), MasterProductProposal.PENDIENTE)) {
                return;
            }
            MasterProductProposal prop = new MasterProductProposal();
            prop.setTenantId(tenantId);
            prop.setCompanyId(companyId);
            prop.setProductId(product.getId());
            prop.setBusinessType(businessType);
            prop.setEan(ean);
            prop.setName(product.getName());
            prop.setCategory(product.getCategory());
            proposalRepository.save(prop);
        } catch (Exception e) {
            log.warn("No se pudo registrar la propuesta al catalogo: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<MasterProductProposal> pending() {
        return proposalRepository.findByStatusOrderByCreatedAtAsc(MasterProductProposal.PENDIENTE);
    }

    /** Aprueba: crea el producto maestro (o reutiliza el del mismo EAN) y vincula al tenant. */
    @Transactional
    public MasterProduct approve(UUID proposalId) {
        MasterProductProposal prop = requirePending(proposalId);

        MasterProduct master = null;
        if (prop.getEan() != null) {
            master = masterProductRepository.findByEan(prop.getEan()).orElse(null);
        }
        if (master == null) {
            master = new MasterProduct();
            master.setEan(prop.getEan());
            master.setName(prop.getName());
            master.setCategory(prop.getCategory());
            master.setActive(true);
            master.setVerified(true);
            master.setPhotoSource("tenant");
            if (prop.getBusinessType() != null) {
                master.getRubros().add(prop.getBusinessType());
            }
            master = masterProductRepository.save(master);
        } else if (prop.getBusinessType() != null && !master.getRubros().contains(prop.getBusinessType())) {
            master.getRubros().add(prop.getBusinessType());
            masterProductRepository.save(master);
        }

        if (prop.getProductId() != null) {
            final UUID masterId = master.getId();
            productRepository.findById(prop.getProductId()).ifPresent(p -> {
                if (p.getTenantId().equals(prop.getTenantId()) && p.getMasterProductId() == null) {
                    p.setMasterProductId(masterId);
                    productRepository.save(p);
                }
            });
        }

        prop.setStatus(MasterProductProposal.APROBADA);
        proposalRepository.save(prop);
        return master;
    }

    @Transactional
    public void reject(UUID proposalId) {
        MasterProductProposal prop = requirePending(proposalId);
        prop.setStatus(MasterProductProposal.RECHAZADA);
        proposalRepository.save(prop);
    }

    private MasterProductProposal requirePending(UUID id) {
        MasterProductProposal prop = proposalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Propuesta no encontrada."));
        if (!MasterProductProposal.PENDIENTE.equals(prop.getStatus())) {
            throw new BadRequestException("La propuesta ya fue revisada.");
        }
        return prop;
    }
}
