package com.sumaup360.erp.einvoicing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.documenttemplate.enums.DocumentStatus;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.model.DocumentSeries;
import com.sumaup360.erp.documenttemplate.model.IssuedDocument;
import com.sumaup360.erp.documenttemplate.repository.DocumentSeriesRepository;
import com.sumaup360.erp.documenttemplate.repository.IssuedDocumentRepository;
import com.sumaup360.erp.documenttemplate.service.DocumentSeriesService;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.domain.SaleItem;
import com.sumaup360.erp.einvoicing.model.CompanyEinvoicingConfig;
import com.sumaup360.erp.einvoicing.nubefact.NubefactClient;
import com.sumaup360.erp.einvoicing.nubefact.NubefactProperties;
import com.sumaup360.erp.einvoicing.repository.CompanyEinvoicingConfigRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.erp.repository.SaleItemRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.security.CryptoService;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
// NubeFact exige la fecha en formato DD-MM-YYYY (no ISO).
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Emision de comprobantes electronicos (boleta/factura) via NubeFact, a partir de una
 * venta del POS. Resuelve la empresa por la sucursal de la venta, arma el payload con
 * base+IGV autoconsistente, consume el correlativo y persiste el resultado en
 * erp.issued_documents (xml/cdr/qr/hash/estado SUNAT).
 */
@Service
public class EinvoiceService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter NUBEFACT_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
    private static final BigDecimal IGV_RATE = new BigDecimal("0.18");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final DocumentSeriesService documentSeriesService;
    private final DocumentSeriesRepository documentSeriesRepository;
    private final IssuedDocumentRepository issuedDocumentRepository;
    private final CompanyEinvoicingConfigRepository configRepository;
    private final NubefactClient nubefactClient;
    private final NubefactProperties props;
    private final CryptoService cryptoService;
    private final ObjectMapper mapper;

    public EinvoiceService(SaleRepository saleRepository,
                           SaleItemRepository saleItemRepository,
                           ProductRepository productRepository,
                           CustomerRepository customerRepository,
                           BranchRepository branchRepository,
                           CompanyRepository companyRepository,
                           DocumentSeriesService documentSeriesService,
                           DocumentSeriesRepository documentSeriesRepository,
                           IssuedDocumentRepository issuedDocumentRepository,
                           CompanyEinvoicingConfigRepository configRepository,
                           NubefactClient nubefactClient,
                           NubefactProperties props,
                           CryptoService cryptoService,
                           ObjectMapper mapper) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.documentSeriesService = documentSeriesService;
        this.documentSeriesRepository = documentSeriesRepository;
        this.issuedDocumentRepository = issuedDocumentRepository;
        this.configRepository = configRepository;
        this.nubefactClient = nubefactClient;
        this.props = props;
        this.cryptoService = cryptoService;
        this.mapper = mapper;
    }

    // --- Configuracion de credenciales por empresa ---

    @Transactional(readOnly = true)
    public CompanyEinvoicingConfig getConfig(UUID tenantId, UUID companyId) {
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
        return configRepository.findByTenantIdAndCompanyId(tenantId, companyId).orElse(null);
    }

    @Transactional
    public CompanyEinvoicingConfig saveConfig(UUID tenantId, UUID companyId, String ruta, String token, boolean enabled) {
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
        CompanyEinvoicingConfig cfg = configRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseGet(() -> {
                    CompanyEinvoicingConfig c = new CompanyEinvoicingConfig();
                    c.setTenantId(tenantId);
                    c.setCompanyId(companyId);
                    c.setProvider("NUBEFACT");
                    return c;
                });
        if (notBlank(ruta)) {
            cfg.setNubefactRuta(ruta.trim());
        }
        if (notBlank(token)) {
            cfg.setNubefactTokenEnc(cryptoService.encrypt(token.trim()));
        }
        cfg.setEnabled(enabled);
        return configRepository.save(cfg);
    }

    // --- Emision ---

    /** Resultado de la emision, listo para la respuesta HTTP. */
    public record EmitResult(String fullNumber, String documentType, String sunatStatus,
                             String pdfUrl, String xmlUrl, String cdrUrl, String qrValue,
                             String hashValue, boolean accepted, String message) {
    }

    @Transactional
    public EmitResult emitFromSale(UUID tenantId, UUID saleId) {
        Sale sale = saleRepository.findByIdAndTenantId(saleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada."));
        Branch branch = branchRepository.findByIdAndTenantId(sale.getBranchId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        UUID companyId = branch.getCompanyId();
        Company company = companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada."));
        if (company.getRuc() == null || company.getRuc().isBlank()) {
            throw new BadRequestException("La empresa no tiene RUC configurado; no puede emitir comprobantes.");
        }

        // Evita re-emitir una venta que ya tiene comprobante aceptado por SUNAT.
        for (IssuedDocument existing : issuedDocumentRepository.findBySaleIdAndTenantId(saleId, tenantId)) {
            if (existing.getDocumentType() != null && existing.getDocumentType().isFiscal()
                    && "ACEPTADO".equals(existing.getSunatStatus())) {
                throw new ConflictException("Esta venta ya tiene un comprobante electronico aceptado ("
                        + existing.getFullNumber() + ").");
            }
        }

        Creds creds = resolveCreds(tenantId, companyId);

        Customer customer = sale.getCustomerId() == null ? null
                : customerRepository.findByIdAndTenantId(sale.getCustomerId(), tenantId).orElse(null);

        boolean factura = customer != null
                && "RUC".equalsIgnoreCase(customer.getDocType())
                && customer.getDocNumber() != null && customer.getDocNumber().length() == 11;
        DocumentType type = factura ? DocumentType.INVOICE : DocumentType.SALE_RECEIPT;
        int tipoComprobante = factura ? 1 : 2; // NubeFact: 1=factura, 2=boleta

        List<SaleItem> items = saleItemRepository.findBySaleId(saleId);
        if (items.isEmpty()) {
            throw new BadRequestException("La venta no tiene items para facturar.");
        }

        DocumentSeriesService.ConsumedNumber num = nextNumber(tenantId, companyId, branch.getId(), type, creds.demo());

        // Items con base+IGV autoconsistente (evita rechazos de SUNAT por cuadre).
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode arr = payload.putArray("items");
        BigDecimal totalGravada = BigDecimal.ZERO;
        BigDecimal totalIgv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (SaleItem it : items) {
            BigDecimal qty = it.getQuantity();
            BigDecimal valorUnit = it.getUnitPrice().divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
            BigDecimal subtotal = valorUnit.multiply(qty).setScale(2, RoundingMode.HALF_UP);
            BigDecimal igv = subtotal.multiply(IGV_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = subtotal.add(igv);
            totalGravada = totalGravada.add(subtotal);
            totalIgv = totalIgv.add(igv);
            total = total.add(lineTotal);

            ObjectNode node = arr.addObject();
            node.put("unidad_de_medida", "NIU");
            node.put("codigo", "");
            node.put("descripcion", itemDescription(it, tenantId));
            node.put("cantidad", qty);
            node.put("valor_unitario", valorUnit);
            node.put("precio_unitario", valorUnit.multiply(IGV_FACTOR).setScale(2, RoundingMode.HALF_UP));
            node.put("descuento", 0);
            node.put("subtotal", subtotal);
            node.put("tipo_de_igv", 1); // Gravado - Operacion Onerosa
            node.put("igv", igv);
            node.put("total", lineTotal);
            node.put("anticipo_regularizacion", false);
        }

        payload.put("operacion", "generar_comprobante");
        payload.put("tipo_de_comprobante", tipoComprobante);
        payload.put("serie", num.series());
        payload.put("numero", num.number());
        payload.put("sunat_transaction", 1);
        payload.put("cliente_tipo_de_documento", clienteTipoDoc(customer));
        payload.put("cliente_numero_de_documento",
                customer != null && customer.getDocNumber() != null ? customer.getDocNumber() : "00000000");
        payload.put("cliente_denominacion",
                customer != null && customer.getName() != null ? customer.getName() : "CLIENTES VARIOS");
        payload.put("cliente_direccion", "");
        payload.put("cliente_email", customer != null && customer.getEmail() != null ? customer.getEmail() : "");
        payload.put("fecha_de_emision", LocalDate.now(LIMA).format(NUBEFACT_DATE));
        payload.put("moneda", 1); // PEN
        payload.put("porcentaje_de_igv", new BigDecimal("18.00"));
        payload.put("total_gravada", totalGravada);
        payload.put("total_igv", totalIgv);
        payload.put("total", total);
        payload.put("enviar_automaticamente_a_la_sunat", true);
        payload.put("enviar_automaticamente_al_cliente", false);
        payload.put("formato_de_pdf", "TICKET");

        NubefactClient.NubefactResult res = nubefactClient.emit(creds.ruta(), creds.token(), payload);
        if (!res.ok()) {
            // La transaccion hace rollback: el correlativo consumido se revierte y se
            // puede reintentar con el mismo numero tras corregir los datos.
            throw new BadRequestException("NubeFact no acepto el comprobante: " + res.errors());
        }

        // El PDF puede venir en enlace_del_pdf o derivarse del enlace base (+ ".pdf").
        String pdfUrl = notBlank(res.enlacePdf()) ? res.enlacePdf()
                : (notBlank(res.enlace()) ? res.enlace() + ".pdf" : null);

        IssuedDocument doc = new IssuedDocument();
        doc.setTenantId(tenantId);
        doc.setCompanyId(companyId);
        doc.setBranchId(branch.getId());
        doc.setDocumentType(type);
        doc.setDocumentStatus(res.aceptadaPorSunat() ? DocumentStatus.ACCEPTED : DocumentStatus.SENT);
        doc.setCustomerId(sale.getCustomerId());
        doc.setSaleId(saleId);
        doc.setSeries(num.series());
        doc.setNumber(num.number());
        doc.setFullNumber(num.fullNumber());
        doc.setSubtotal(totalGravada);
        doc.setIgv(totalIgv);
        doc.setTotal(total);
        doc.setCurrency("PEN");
        doc.setPdfUrl(pdfUrl);
        doc.setXmlUrl(res.enlaceXml());
        doc.setCdrUrl(res.enlaceCdr());
        doc.setQrValue(res.qr());
        doc.setHashValue(res.hash());
        doc.setSunatStatus(res.aceptadaPorSunat() ? "ACEPTADO" : "ENVIADO");
        doc.setIssuedAt(OffsetDateTime.now());
        issuedDocumentRepository.save(doc);

        return new EmitResult(doc.getFullNumber(), type.name(), doc.getSunatStatus(),
                pdfUrl, res.enlaceXml(), res.enlaceCdr(), res.qr(), res.hash(),
                res.aceptadaPorSunat(), res.sunatDescription());
    }

    // --- helpers ---

    private record Creds(String ruta, String token, boolean demo) {
    }

    private Creds resolveCreds(UUID tenantId, UUID companyId) {
        String demoRuta = props.isEnabled() ? props.getDemoRuta() : null;
        Optional<CompanyEinvoicingConfig> cfg = configRepository.findByTenantIdAndCompanyId(tenantId, companyId);
        if (cfg.isPresent() && cfg.get().isEnabled()
                && notBlank(cfg.get().getNubefactRuta()) && notBlank(cfg.get().getNubefactTokenEnc())) {
            String ruta = cfg.get().getNubefactRuta().trim();
            // Aunque venga de la config por empresa, si la ruta es la del demo se trata como demo
            // (la cuenta demo solo tiene registradas las series FFF1/BBB1).
            boolean demo = notBlank(demoRuta) && demoRuta.trim().equals(ruta);
            return new Creds(ruta, cryptoService.decrypt(cfg.get().getNubefactTokenEnc()), demo);
        }
        if (props.isEnabled() && notBlank(props.getDemoRuta()) && notBlank(props.getDemoToken())) {
            return new Creds(props.getDemoRuta(), props.getDemoToken(), true);
        }
        throw new BadRequestException(
                "Configura tus credenciales de NubeFact (ruta y token) para emitir comprobantes.");
    }

    /**
     * Consume el siguiente correlativo del tipo. Si no hay serie configurada, crea una
     * por defecto (F001 factura / B001 boleta) para no bloquear la emision.
     */
    private DocumentSeriesService.ConsumedNumber nextNumber(UUID tenantId, UUID companyId,
                                                            UUID branchId, DocumentType type, boolean demo) {
        Optional<DocumentSeriesService.ConsumedNumber> n =
                documentSeriesService.consumeNext(tenantId, companyId, type);
        if (n.isPresent()) {
            return n.get();
        }
        // En demo, NubeFact solo tiene registradas las series de prueba FFF1/BBB1.
        // Con credenciales reales el emisor registra sus propias series (F001/B001...).
        String defSeries = demo
                ? (type == DocumentType.INVOICE ? "FFF1" : "BBB1")
                : (type == DocumentType.INVOICE ? "F001" : "B001");
        if (documentSeriesRepository.existsByTenantIdAndCompanyIdAndDocumentTypeAndSeries(
                tenantId, companyId, type, defSeries)) {
            throw new BadRequestException("Tu serie " + defSeries
                    + " esta inactiva. Activala en Series de documentos para poder emitir.");
        }
        DocumentSeries s = new DocumentSeries();
        s.setTenantId(tenantId);
        s.setCompanyId(companyId);
        s.setBranchId(branchId);
        s.setDocumentType(type);
        s.setSeries(defSeries);
        s.setCurrentNumber(0);
        s.setActive(true);
        documentSeriesRepository.save(s);
        return documentSeriesService.consumeNext(tenantId, companyId, type)
                .orElseThrow(() -> new BadRequestException("No se pudo asignar la numeracion del comprobante."));
    }

    private String itemDescription(SaleItem it, UUID tenantId) {
        if (it.getDescription() != null && !it.getDescription().isBlank()) {
            return it.getDescription();
        }
        if (it.getProductId() != null) {
            return productRepository.findByIdAndTenantId(it.getProductId(), tenantId)
                    .map(Product::getName)
                    .orElse("Producto");
        }
        return "Producto";
    }

    private String clienteTipoDoc(Customer c) {
        if (c == null || c.getDocType() == null) {
            return "-";
        }
        return switch (c.getDocType().toUpperCase()) {
            case "RUC" -> "6";
            case "DNI" -> "1";
            case "CE" -> "4";
            case "PASSPORT", "PAS" -> "7";
            default -> "-";
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
