package com.sumaup360.erp.documenttemplate.controller;

import com.sumaup360.erp.documenttemplate.dto.DocumentSeriesDtos.CreateSeriesRequest;
import com.sumaup360.erp.documenttemplate.dto.DocumentSeriesDtos.SeriesResponse;
import com.sumaup360.erp.documenttemplate.service.DocumentSeriesService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Series y correlativos de documentos. Multi-tenant + company. */
@RestController
@RequestMapping("/api/v1/erp/document-series")
@Tag(name = "Series de documentos", description = "Series y correlativos por tipo de documento")
public class DocumentSeriesController {

    private final DocumentSeriesService seriesService;

    public DocumentSeriesController(DocumentSeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('document-series:manage')")
    @Operation(summary = "Crea una serie (requiere document-series:manage)")
    public SeriesResponse create(@Valid @RequestBody CreateSeriesRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return SeriesResponse.from(seriesService.create(tenantId, req));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('document-series:read')")
    @Operation(summary = "Lista las series de la empresa (requiere document-series:read)")
    public List<SeriesResponse> list(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return seriesService.list(tenantId, companyId).stream().map(SeriesResponse::from).toList();
    }
}
