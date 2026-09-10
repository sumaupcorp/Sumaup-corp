package com.sumaup360.erp.documenttemplate.controller;

import com.sumaup360.erp.documenttemplate.dto.DocumentData;
import com.sumaup360.erp.documenttemplate.dto.DocumentPreviewRequest;
import com.sumaup360.erp.documenttemplate.dto.DocumentTemplateRequest;
import com.sumaup360.erp.documenttemplate.dto.DocumentTemplateResponse;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.renderer.DocumentHtmlRendererService;
import com.sumaup360.erp.documenttemplate.service.DocumentSampleDataFactory;
import com.sumaup360.erp.documenttemplate.service.DocumentTemplateService;
import com.sumaup360.erp.documenttemplate.service.DocumentTemplateService.ResolvedTemplate;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/** Plantillas de documentos. Multi-tenant: tenant del contexto + companyId explicito. */
@RestController
@RequestMapping("/api/v1/erp/document-templates")
@Tag(name = "Plantillas de documentos", description = "Configuracion visual de documentos comerciales")
public class DocumentTemplateController {

    private final DocumentTemplateService templateService;
    private final DocumentSampleDataFactory sampleDataFactory;
    private final DocumentHtmlRendererService htmlRenderer;

    public DocumentTemplateController(DocumentTemplateService templateService,
                                      DocumentSampleDataFactory sampleDataFactory,
                                      DocumentHtmlRendererService htmlRenderer) {
        this.templateService = templateService;
        this.sampleDataFactory = sampleDataFactory;
        this.htmlRenderer = htmlRenderer;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('document-template:read')")
    @Operation(summary = "Lista plantillas de la empresa (requiere document-template:read)")
    public List<DocumentTemplateResponse> list(@RequestParam UUID companyId,
                                               @RequestParam(required = false) DocumentType documentType) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return templateService.list(tenantId, companyId, documentType).stream()
                .map(DocumentTemplateResponse::from).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('document-template:read')")
    @Operation(summary = "Detalle de plantilla (requiere document-template:read)")
    public DocumentTemplateResponse get(@PathVariable UUID id, @RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return DocumentTemplateResponse.from(templateService.get(tenantId, companyId, id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('document-template:manage')")
    @Operation(summary = "Crea una plantilla (requiere document-template:manage)")
    public DocumentTemplateResponse create(@Valid @RequestBody DocumentTemplateRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return DocumentTemplateResponse.from(templateService.create(tenantId, req, userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('document-template:manage')")
    @Operation(summary = "Actualiza una plantilla (requiere document-template:manage)")
    public DocumentTemplateResponse update(@PathVariable UUID id,
                                           @Valid @RequestBody DocumentTemplateRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return DocumentTemplateResponse.from(templateService.update(tenantId, id, req, userId));
    }

    @PostMapping("/{id}/set-default")
    @PreAuthorize("hasAuthority('document-template:manage')")
    @Operation(summary = "Marca la plantilla como predeterminada (requiere document-template:manage)")
    public DocumentTemplateResponse setDefault(@PathVariable UUID id, @RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return DocumentTemplateResponse.from(templateService.setDefault(tenantId, companyId, id, userId));
    }

    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('document:render')")
    @Operation(summary = "Genera un preview HTML con datos de ejemplo; config inline pisa la guardada (requiere document:render)")
    public ResponseEntity<String> preview(@RequestBody DocumentPreviewRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        ResolvedTemplate resolved = templateService.resolve(
                tenantId, req.templateId(), req.companyId(), req.documentType(), req.printFormat());
        applyInline(resolved, req.config());
        DocumentData sample = sampleDataFactory.build(resolved.documentType(), req.businessType());
        String html = htmlRenderer.renderHtml(resolved.format(), sample, resolved.config());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    /** Preview en vivo: la configuracion sin guardar del editor pisa la resuelta. */
    private void applyInline(ResolvedTemplate resolved, DocumentPreviewRequest.InlineConfig i) {
        if (i == null) {
            return;
        }
        var c = resolved.config();
        if (i.primaryColor() != null) c.setPrimaryColor(i.primaryColor());
        if (i.secondaryColor() != null) c.setSecondaryColor(i.secondaryColor());
        if (i.logoUrl() != null) c.setLogoUrl(i.logoUrl().isBlank() ? null : i.logoUrl());
        if (i.showLogo() != null) c.setShowLogo(i.showLogo());
        if (i.showQr() != null) c.setShowQr(i.showQr());
        if (i.showPaymentInfo() != null) c.setShowPaymentInfo(i.showPaymentInfo());
        if (i.showSeller() != null) c.setShowSeller(i.showSeller());
        if (i.showCustomerAddress() != null) c.setShowCustomerAddress(i.showCustomerAddress());
        if (i.showBusinessExtraFields() != null) c.setShowBusinessExtraFields(i.showBusinessExtraFields());
        if (i.footerText() != null) c.setFooterText(i.footerText());
        if (i.legalMessage() != null) c.setLegalMessage(i.legalMessage());
        if (i.commercialMessage() != null) c.setCommercialMessage(i.commercialMessage());
        if (i.thankYouMessage() != null) c.setThankYouMessage(i.thankYouMessage());
    }
}
