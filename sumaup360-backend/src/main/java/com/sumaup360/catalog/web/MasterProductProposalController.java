package com.sumaup360.catalog.web;

import com.sumaup360.catalog.domain.MasterProductProposal;
import com.sumaup360.catalog.dto.MasterProductDtos.MasterProductView;
import com.sumaup360.catalog.service.MasterProductProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Curacion de propuestas de los tenants al catalogo maestro (staff). */
@RestController
@RequestMapping("/api/v1/backoffice/catalog/proposals")
@Tag(name = "Backoffice - Propuestas de catalogo", description = "Productos propuestos por los negocios")
public class MasterProductProposalController {

    private final MasterProductProposalService proposalService;

    public MasterProductProposalController(MasterProductProposalService proposalService) {
        this.proposalService = proposalService;
    }

    public record ProposalView(UUID id, String name, String ean, String category,
                               String businessType, OffsetDateTime createdAt) {
        static ProposalView from(MasterProductProposal p) {
            return new ProposalView(p.getId(), p.getName(), p.getEan(), p.getCategory(),
                    p.getBusinessType(), p.getCreatedAt());
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Propuestas pendientes de los negocios")
    public List<ProposalView> pending() {
        return proposalService.pending().stream().map(ProposalView::from).toList();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Aprueba: crea el producto maestro y vincula el producto del negocio")
    public MasterProductView approve(@PathVariable UUID id) {
        return MasterProductView.from(proposalService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Rechaza la propuesta")
    public void reject(@PathVariable UUID id) {
        proposalService.reject(id);
    }
}
