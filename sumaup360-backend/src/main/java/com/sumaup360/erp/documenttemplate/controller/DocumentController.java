package com.sumaup360.erp.documenttemplate.controller;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.documenttemplate.dto.DocumentData;
import com.sumaup360.erp.documenttemplate.dto.DocumentRenderRequest;
import com.sumaup360.erp.documenttemplate.model.IssuedDocument;
import com.sumaup360.erp.documenttemplate.renderer.DocumentHtmlRendererService;
import com.sumaup360.erp.documenttemplate.renderer.DocumentPdfService;
import com.sumaup360.erp.documenttemplate.repository.IssuedDocumentRepository;
import com.sumaup360.erp.documenttemplate.service.DocumentTemplateService;
import com.sumaup360.erp.documenttemplate.service.DocumentTemplateService.ResolvedTemplate;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Generacion de PDF de documentos (fuente oficial en backend). */
@RestController
@RequestMapping("/api/v1/erp/documents")
@Tag(name = "Documentos", description = "Render de PDF de documentos comerciales")
public class DocumentController {

    private final DocumentTemplateService templateService;
    private final DocumentHtmlRendererService htmlRenderer;
    private final DocumentPdfService pdfService;
    private final IssuedDocumentRepository issuedDocumentRepository;

    public DocumentController(DocumentTemplateService templateService,
                              DocumentHtmlRendererService htmlRenderer,
                              DocumentPdfService pdfService,
                              IssuedDocumentRepository issuedDocumentRepository) {
        this.templateService = templateService;
        this.htmlRenderer = htmlRenderer;
        this.pdfService = pdfService;
        this.issuedDocumentRepository = issuedDocumentRepository;
    }

    @PostMapping(value = "/render-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('document:render')")
    @Operation(summary = "Genera el PDF de un documento desde una plantilla y datos (requiere document:render)")
    public ResponseEntity<byte[]> renderPdf(@Valid @RequestBody DocumentRenderRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        ResolvedTemplate resolved = templateService.resolve(
                tenantId, req.templateId(), req.companyId(), req.documentType(), req.printFormat());

        DocumentData data = req.data();
        if (data.getMeta() == null) {
            throw new BadRequestException("Faltan los metadatos del documento (meta).");
        }
        if (data.getMeta().getDocumentTypeLabel() == null) {
            data.getMeta().setDocumentTypeLabel(resolved.documentType().getLabel());
        }

        String html = htmlRenderer.renderHtml(resolved.format(), data, resolved.config());
        byte[] pdf = pdfService.htmlToPdf(html);

        String fileName = "documento.pdf";
        if (data.getMeta().getFullNumber() != null) {
            fileName = data.getMeta().getFullNumber() + ".pdf";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());
        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('document:render')")
    @Operation(summary = "Descarga el PDF de un documento emitido (requiere document:render)")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        IssuedDocument doc = issuedDocumentRepository.findById(id)
                .filter(d -> d.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado."));
        // TODO SUNAT/storage: la emision persiste el PDF (pdf_url) al integrar storage y SUNAT.
        // Por ahora la generacion oficial se hace via POST /render-pdf.
        throw new BadRequestException(
                "La descarga por id estara disponible cuando se implemente la emision y el storage "
                        + "de PDFs (fase SUNAT). Usa /render-pdf para generar el PDF. (doc " + doc.getId() + ")");
    }
}
