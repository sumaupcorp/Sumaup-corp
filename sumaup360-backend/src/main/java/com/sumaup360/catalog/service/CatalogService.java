package com.sumaup360.catalog.service;

import com.sumaup360.catalog.domain.BusinessType;
import com.sumaup360.catalog.dto.CatalogDtos.CodeName;
import com.sumaup360.catalog.dto.CatalogDtos.CurrencyView;
import com.sumaup360.catalog.dto.CatalogDtos.ModuleView;
import com.sumaup360.catalog.dto.CatalogDtos.VerticalView;
import com.sumaup360.catalog.repository.BusinessTypeRepository;
import com.sumaup360.catalog.repository.CurrencyRepository;
import com.sumaup360.catalog.repository.DocumentTypeCatalogRepository;
import com.sumaup360.catalog.repository.ModuleCatalogRepository;
import com.sumaup360.catalog.repository.PersonSegmentRepository;
import com.sumaup360.catalog.repository.VerticalRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lectura de los catalogos compartidos. Datos globales (no dependen de tenant). */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CurrencyRepository currencyRepository;
    private final DocumentTypeCatalogRepository documentTypeRepository;
    private final BusinessTypeRepository businessTypeRepository;
    private final VerticalRepository verticalRepository;
    private final ModuleCatalogRepository moduleRepository;
    private final PersonSegmentRepository personSegmentRepository;

    public CatalogService(CurrencyRepository currencyRepository,
                          DocumentTypeCatalogRepository documentTypeRepository,
                          BusinessTypeRepository businessTypeRepository,
                          VerticalRepository verticalRepository,
                          ModuleCatalogRepository moduleRepository,
                          PersonSegmentRepository personSegmentRepository) {
        this.currencyRepository = currencyRepository;
        this.documentTypeRepository = documentTypeRepository;
        this.businessTypeRepository = businessTypeRepository;
        this.verticalRepository = verticalRepository;
        this.moduleRepository = moduleRepository;
        this.personSegmentRepository = personSegmentRepository;
    }

    public List<CurrencyView> currencies() {
        return currencyRepository.findByActiveTrue().stream()
                .map(c -> new CurrencyView(c.getCode(), c.getName(), c.getSymbol())).toList();
    }

    public List<CodeName> documentTypes() {
        return documentTypeRepository.findByActiveTrue().stream()
                .map(d -> new CodeName(d.getCode(), d.getName())).toList();
    }

    public List<CodeName> businessTypes() {
        return businessTypeRepository.findByActiveTrue().stream()
                .map(b -> new CodeName(b.getCode(), b.getName())).toList();
    }

    public List<CodeName> personSegments() {
        return personSegmentRepository.findByActiveTrue().stream()
                .map(s -> new CodeName(s.getCode(), s.getName())).toList();
    }

    public List<ModuleView> modules() {
        return moduleRepository.findByActiveTrue().stream()
                .map(m -> new ModuleView(m.getCode(), m.getName(), m.isCore())).toList();
    }

    /** Verticales (todas, o filtradas por codigo de rubro). */
    public List<VerticalView> verticals(String businessTypeCode) {
        Map<Long, String> codeById = businessTypeRepository.findAll().stream()
                .collect(Collectors.toMap(BusinessType::getId, BusinessType::getCode));

        var verticals = (businessTypeCode == null || businessTypeCode.isBlank())
                ? verticalRepository.findByActiveTrue()
                : verticalRepository.findByActiveTrueAndBusinessTypeId(
                        businessTypeRepository.findByCode(businessTypeCode)
                                .orElseThrow(() -> new ResourceNotFoundException("Rubro no encontrado."))
                                .getId());

        return verticals.stream()
                .map(v -> new VerticalView(v.getCode(), v.getName(), codeById.get(v.getBusinessTypeId())))
                .toList();
    }
}
