package com.sumaup360.erp.service;

import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.documenttemplate.dto.DocumentData;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.enums.PrintFormat;
import com.sumaup360.erp.documenttemplate.model.DocumentTemplate;
import com.sumaup360.erp.documenttemplate.model.IssuedDocument;
import com.sumaup360.erp.documenttemplate.renderer.DocumentHtmlRendererService;
import com.sumaup360.erp.documenttemplate.renderer.DocumentPdfService;
import com.sumaup360.erp.documenttemplate.repository.DocumentTemplateRepository;
import com.sumaup360.erp.documenttemplate.repository.IssuedDocumentRepository;
import com.sumaup360.erp.documenttemplate.service.DocumentTemplateService;
import com.sumaup360.erp.documenttemplate.service.DocumentTemplateService.ResolvedTemplate;
import com.sumaup360.erp.documenttemplate.service.SpanishMoneyWords;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.domain.SaleItem;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.erp.repository.SaleItemRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ticket de una venta: arma los datos del documento desde la venta registrada y lo
 * renderiza con la plantilla que el negocio configuro (o el diseno por defecto termico
 * de 80mm). La fuente de los datos es SIEMPRE la venta persistida, nunca el cliente.
 */
@Service
public class SaleTicketService {

    /** Ticket renderizado con su numero (si la empresa tiene serie configurada). */
    public record RenderedTicket(String html, String fullNumber) {
    }

    private static final DateTimeFormatter ISSUE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final Map<String, String> PAYMENT_LABELS = Map.of(
            "CASH", "Efectivo", "CARD", "Tarjeta", "YAPE", "Yape",
            "PLIN", "Plin", "TRANSFER", "Transferencia");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final AppUserRepository userRepository;
    private final IssuedDocumentRepository issuedDocumentRepository;
    private final DocumentTemplateRepository templateRepository;
    private final DocumentTemplateService templateService;
    private final DocumentHtmlRendererService htmlRenderer;
    private final DocumentPdfService pdfService;

    public SaleTicketService(SaleRepository saleRepository,
                             SaleItemRepository saleItemRepository,
                             ProductRepository productRepository,
                             CustomerRepository customerRepository,
                             BranchRepository branchRepository,
                             CompanyRepository companyRepository,
                             AppUserRepository userRepository,
                             IssuedDocumentRepository issuedDocumentRepository,
                             DocumentTemplateRepository templateRepository,
                             DocumentTemplateService templateService,
                             DocumentHtmlRendererService htmlRenderer,
                             DocumentPdfService pdfService) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.issuedDocumentRepository = issuedDocumentRepository;
        this.templateRepository = templateRepository;
        this.templateService = templateService;
        this.htmlRenderer = htmlRenderer;
        this.pdfService = pdfService;
    }

    @Transactional(readOnly = true)
    public RenderedTicket renderHtml(UUID tenantId, UUID saleId) {
        TicketContext ctx = load(tenantId, saleId);
        String html = htmlRenderer.renderHtml(ctx.resolved.format(), ctx.data, ctx.resolved.config());
        return new RenderedTicket(html, ctx.fullNumber);
    }

    @Transactional(readOnly = true)
    public byte[] renderPdf(UUID tenantId, UUID saleId) {
        TicketContext ctx = load(tenantId, saleId);
        String html = htmlRenderer.renderHtml(ctx.resolved.format(), ctx.data, ctx.resolved.config());
        return pdfService.htmlToPdf(html);
    }

    private record TicketContext(DocumentData data, ResolvedTemplate resolved, String fullNumber) {
    }

    private TicketContext load(UUID tenantId, UUID saleId) {
        Sale sale = saleRepository.findByIdAndTenantId(saleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada."));
        Branch branch = branchRepository.findByIdAndTenantId(sale.getBranchId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        Company company = companyRepository.findByIdAndTenantId(branch.getCompanyId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada."));
        List<SaleItem> items = saleItemRepository.findBySaleId(sale.getId());
        IssuedDocument doc = issuedDocumentRepository
                .findFirstBySaleIdAndTenantIdOrderByCreatedAtDesc(sale.getId(), tenantId)
                .orElse(null);

        // Plantilla: la marcada por defecto para tickets de la empresa; si no hay, defaults 80mm.
        UUID templateId = templateRepository
                .findByTenantIdAndCompanyIdAndDocumentType(tenantId, company.getId(), DocumentType.POS_TICKET)
                .stream()
                .filter(DocumentTemplate::isDefaultTemplate)
                .findFirst()
                .map(DocumentTemplate::getId)
                .orElse(null);
        ResolvedTemplate resolved = templateService.resolve(
                tenantId, templateId, company.getId(), DocumentType.POS_TICKET,
                templateId == null ? PrintFormat.THERMAL_80MM : null);

        DocumentData data = buildData(sale, items, branch, company, doc);
        String fullNumber = doc != null ? doc.getFullNumber() : null;
        return new TicketContext(data, resolved, fullNumber);
    }

    private DocumentData buildData(Sale sale, List<SaleItem> items, Branch branch,
                                   Company company, IssuedDocument doc) {
        DocumentData.Customer customerBlock = null;
        if (sale.getCustomerId() != null) {
            Customer c = customerRepository.findByIdAndTenantId(sale.getCustomerId(), sale.getTenantId())
                    .orElse(null);
            if (c != null) {
                customerBlock = DocumentData.Customer.builder()
                        .docType(c.getDocType())
                        .docNumber(c.getDocNumber())
                        .name(c.getName())
                        .build();
            }
        }

        String cashier = sale.getCreatedBy() != null
                ? userRepository.findById(sale.getCreatedBy())
                    .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
                    .orElse(null)
                : null;

        List<DocumentData.Item> lines = items.stream().map(i -> {
            // Lineas de precio libre (sin producto) llevan su propia descripcion.
            Product p = i.getProductId() != null
                    ? productRepository.findById(i.getProductId()).orElse(null) : null;
            String fallback = i.getDescription() != null ? i.getDescription() : "Producto";
            return DocumentData.Item.builder()
                    .code(p != null ? p.getSku() : null)
                    .description(p != null ? p.getName() : fallback)
                    .unit(p != null ? p.getUnit() : "UNIDAD")
                    .quantity(i.getQuantity())
                    .unitPrice(i.getUnitPrice())
                    .total(i.getLineTotal())
                    .build();
        }).toList();

        BigDecimal total = sale.getTotal();
        BigDecimal subtotal = doc != null ? doc.getSubtotal()
                : total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = doc != null ? doc.getIgv() : total.subtract(subtotal);

        return DocumentData.builder()
                .company(DocumentData.Company.builder()
                        .razonSocial(company.getLegalName())
                        .ruc(company.getRuc())
                        .direccionSucursal(branch.getAddress() != null
                                ? branch.getName() + " - " + branch.getAddress() : branch.getName())
                        .build())
                .customer(customerBlock)
                .meta(DocumentData.Meta.builder()
                        .documentTypeLabel(DocumentType.POS_TICKET.getLabel())
                        .series(doc != null ? doc.getSeries() : null)
                        .number(doc != null && doc.getNumber() != null
                                ? String.valueOf(doc.getNumber()) : null)
                        .fullNumber(doc != null && doc.getFullNumber() != null
                                ? doc.getFullNumber()
                                : "T-" + sale.getId().toString().substring(0, 8).toUpperCase())
                        .issueDate(sale.getCreatedAt() != null
                                ? sale.getCreatedAt().atZoneSameInstant(LIMA).format(ISSUE_FORMAT)
                                : null)
                        .currencyCode("PEN")
                        .currencySymbol("S/")
                        .paymentMethod(PAYMENT_LABELS.getOrDefault(sale.getPaymentMethod(), sale.getPaymentMethod()))
                        .cashier(cashier)
                        .build())
                .items(lines)
                .totals(DocumentData.Totals.builder()
                        .subtotal(subtotal)
                        .igv(igv)
                        .total(total)
                        .amountInWords(SpanishMoneyWords.amountInWords(total))
                        .build())
                .build();
    }
}
