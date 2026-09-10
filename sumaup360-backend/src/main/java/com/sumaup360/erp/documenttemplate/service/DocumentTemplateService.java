package com.sumaup360.erp.documenttemplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.documenttemplate.dto.DocumentTemplateRequest;
import com.sumaup360.erp.documenttemplate.dto.DocumentTemplateResponse;
import com.sumaup360.erp.documenttemplate.dto.TemplateConfig;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import com.sumaup360.erp.documenttemplate.model.DocumentTemplate;
import com.sumaup360.erp.documenttemplate.model.DocumentTemplateAudit;
import com.sumaup360.erp.documenttemplate.repository.DocumentTemplateAuditRepository;
import com.sumaup360.erp.documenttemplate.repository.DocumentTemplateRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Gestion de plantillas de documentos (multi-tenant + company) con auditoria de cambios. */
@Service
public class DocumentTemplateService {

    /** Configuracion + formato + tipo resueltos para render/preview. */
    public record ResolvedTemplate(PrintFormat format, DocumentType documentType, TemplateConfig config) {
    }

    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateAuditRepository auditRepository;
    private final CompanyRepository companyRepository;
    private final TemplateConfigFactory configFactory;
    private final ObjectMapper objectMapper;

    public DocumentTemplateService(DocumentTemplateRepository templateRepository,
                                   DocumentTemplateAuditRepository auditRepository,
                                   CompanyRepository companyRepository,
                                   TemplateConfigFactory configFactory,
                                   ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.auditRepository = auditRepository;
        this.companyRepository = companyRepository;
        this.configFactory = configFactory;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<DocumentTemplate> list(UUID tenantId, UUID companyId, DocumentType documentType) {
        requireCompany(tenantId, companyId);
        return documentType == null
                ? templateRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                : templateRepository.findByTenantIdAndCompanyIdAndDocumentType(tenantId, companyId, documentType);
    }

    @Transactional(readOnly = true)
    public DocumentTemplate get(UUID tenantId, UUID companyId, UUID id) {
        return templateRepository.findByIdAndTenantIdAndCompanyId(id, tenantId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada."));
    }

    @Transactional
    public DocumentTemplate create(UUID tenantId, DocumentTemplateRequest req, UUID userId) {
        requireCompany(tenantId, req.companyId());
        if (templateRepository.existsByTenantIdAndCompanyIdAndTemplateCode(
                tenantId, req.companyId(), req.templateCode())) {
            throw new ConflictException("Ya existe una plantilla con el codigo '" + req.templateCode() + "'.");
        }
        DocumentTemplate t = new DocumentTemplate();
        t.setTenantId(tenantId);
        apply(t, req);
        t.setActive(true);
        DocumentTemplate saved = templateRepository.save(t);
        audit(saved, "CREATE", null, userId);
        return saved;
    }

    @Transactional
    public DocumentTemplate update(UUID tenantId, UUID id, DocumentTemplateRequest req, UUID userId) {
        DocumentTemplate t = templateRepository
                .findByIdAndTenantIdAndCompanyId(id, tenantId, req.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada."));
        String previous = toJson(DocumentTemplateResponse.from(t));
        apply(t, req);
        DocumentTemplate saved = templateRepository.save(t);
        auditWithPrevious(saved, "UPDATE", previous, userId);
        return saved;
    }

    @Transactional
    public DocumentTemplate setDefault(UUID tenantId, UUID companyId, UUID id, UUID userId) {
        DocumentTemplate target = get(tenantId, companyId, id);
        templateRepository
                .findByTenantIdAndCompanyIdAndDocumentType(tenantId, companyId, target.getDocumentType())
                .forEach(other -> {
                    if (other.isDefaultTemplate() && !other.getId().equals(target.getId())) {
                        other.setDefaultTemplate(false);
                        templateRepository.save(other);
                    }
                });
        target.setDefaultTemplate(true);
        DocumentTemplate saved = templateRepository.save(target);
        audit(saved, "SET_DEFAULT", null, userId);
        return saved;
    }

    /** Resuelve la config para preview/render: desde plantilla si hay templateId, si no defaults. */
    @Transactional(readOnly = true)
    public ResolvedTemplate resolve(UUID tenantId, UUID templateId, UUID companyId,
                                    DocumentType documentType, PrintFormat printFormat) {
        if (templateId != null) {
            if (companyId == null) {
                throw new ResourceNotFoundException("companyId es requerido al usar templateId.");
            }
            DocumentTemplate t = get(tenantId, companyId, templateId);
            PrintFormat format = printFormat != null ? printFormat : t.getPrintFormat();
            return new ResolvedTemplate(format, t.getDocumentType(), configFactory.fromTemplate(t));
        }
        DocumentType type = documentType != null ? documentType : DocumentType.SALE_RECEIPT;
        PrintFormat format = printFormat != null ? printFormat : PrintFormat.A4;
        return new ResolvedTemplate(format, type, configFactory.defaults(type, format));
    }

    // ---- helpers ----

    private void requireCompany(UUID tenantId, UUID companyId) {
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }

    private void apply(DocumentTemplate t, DocumentTemplateRequest req) {
        t.setCompanyId(req.companyId());
        t.setBranchId(req.branchId());
        t.setBusinessTypeId(req.businessTypeId());
        t.setDocumentType(req.documentType());
        t.setPrintFormat(req.printFormat());
        t.setTemplateName(req.templateName());
        t.setTemplateCode(req.templateCode());
        if (notBlank(req.primaryColor())) t.setPrimaryColor(req.primaryColor());
        if (notBlank(req.secondaryColor())) t.setSecondaryColor(req.secondaryColor());
        if (notBlank(req.fontFamily())) t.setFontFamily(req.fontFamily());
        t.setLogoUrl(req.logoUrl());
        if (req.showLogo() != null) t.setShowLogo(req.showLogo());
        if (req.showQr() != null) t.setShowQr(req.showQr());
        if (req.showPaymentInfo() != null) t.setShowPaymentInfo(req.showPaymentInfo());
        if (req.showSeller() != null) t.setShowSeller(req.showSeller());
        if (req.showCustomerAddress() != null) t.setShowCustomerAddress(req.showCustomerAddress());
        if (req.showBusinessExtraFields() != null) t.setShowBusinessExtraFields(req.showBusinessExtraFields());
        t.setHeaderConfig(req.headerConfig());
        t.setBodyConfig(req.bodyConfig());
        t.setFooterConfig(req.footerConfig());
        t.setCustomCss(req.customCss());
    }

    private void audit(DocumentTemplate t, String action, String previous, UUID userId) {
        auditWithPrevious(t, action, previous, userId);
    }

    private void auditWithPrevious(DocumentTemplate t, String action, String previous, UUID userId) {
        DocumentTemplateAudit a = new DocumentTemplateAudit();
        a.setTenantId(t.getTenantId());
        a.setCompanyId(t.getCompanyId());
        a.setTemplateId(t.getId());
        a.setAction(action);
        a.setPreviousData(previous);
        a.setNewData(toJson(DocumentTemplateResponse.from(t)));
        a.setUserId(userId);
        auditRepository.save(a);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
