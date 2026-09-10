package com.sumaup360.erp.documenttemplate.service;

import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.documenttemplate.dto.DocumentSeriesDtos.CreateSeriesRequest;
import com.sumaup360.erp.documenttemplate.model.DocumentSeries;
import com.sumaup360.erp.documenttemplate.repository.DocumentSeriesRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Series y correlativos por tipo de documento (preparado para SUNAT). */
@Service
public class DocumentSeriesService {

    private final DocumentSeriesRepository seriesRepository;
    private final CompanyRepository companyRepository;

    public DocumentSeriesService(DocumentSeriesRepository seriesRepository,
                                 CompanyRepository companyRepository) {
        this.seriesRepository = seriesRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public DocumentSeries create(UUID tenantId, CreateSeriesRequest req) {
        requireCompany(tenantId, req.companyId());
        if (seriesRepository.existsByTenantIdAndCompanyIdAndDocumentTypeAndSeries(
                tenantId, req.companyId(), req.documentType(), req.series())) {
            throw new ConflictException("Ya existe la serie '" + req.series()
                    + "' para ese tipo de documento.");
        }
        DocumentSeries s = new DocumentSeries();
        s.setTenantId(tenantId);
        s.setCompanyId(req.companyId());
        s.setBranchId(req.branchId());
        s.setDocumentType(req.documentType());
        s.setSeries(req.series());
        s.setCurrentNumber(0);
        s.setActive(req.active() == null || req.active());
        return seriesRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<DocumentSeries> list(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        return seriesRepository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    /** Numero consumido de una serie: B001 + 123 -> "B001-00000123". */
    public record ConsumedNumber(String series, long number, String fullNumber) {
    }

    /**
     * Consume el siguiente correlativo de la primera serie activa del tipo (bloqueo por
     * fila: dos cajas emitiendo a la vez no repiten numero). Vacio si no hay serie
     * configurada; el documento se emite igual, sin numeracion oficial.
     */
    @Transactional
    public java.util.Optional<ConsumedNumber> consumeNext(UUID tenantId, UUID companyId,
                                                          com.sumaup360.erp.documenttemplate.enums.DocumentType type) {
        List<DocumentSeries> series = seriesRepository.findActiveForUpdate(tenantId, companyId, type);
        if (series.isEmpty()) {
            return java.util.Optional.empty();
        }
        DocumentSeries s = series.get(0);
        long next = s.getCurrentNumber() + 1;
        s.setCurrentNumber(next);
        seriesRepository.save(s);
        return java.util.Optional.of(new ConsumedNumber(
                s.getSeries(), next, "%s-%08d".formatted(s.getSeries(), next)));
    }

    private void requireCompany(UUID tenantId, UUID companyId) {
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }
}
