package com.sumaup360.erp.accounting.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.accounting.model.AccountingConfig;
import com.sumaup360.erp.accounting.model.JournalEntry;
import com.sumaup360.erp.accounting.model.JournalEntryLine;
import com.sumaup360.erp.accounting.repository.JournalEntryLineRepository;
import com.sumaup360.erp.accounting.repository.JournalEntryRepository;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.model.IssuedDocument;
import com.sumaup360.erp.documenttemplate.repository.IssuedDocumentRepository;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Asientos contables: creacion manual (cuadrada) y autogenerada desde una venta. */
@Service
public class JournalEntryService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");

    private final JournalEntryRepository entryRepository;
    private final JournalEntryLineRepository lineRepository;
    private final AccountingService accountingService;
    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final CustomerRepository customerRepository;
    private final IssuedDocumentRepository issuedDocumentRepository;

    public JournalEntryService(JournalEntryRepository entryRepository,
                               JournalEntryLineRepository lineRepository,
                               AccountingService accountingService,
                               SaleRepository saleRepository,
                               BranchRepository branchRepository,
                               CustomerRepository customerRepository,
                               IssuedDocumentRepository issuedDocumentRepository) {
        this.entryRepository = entryRepository;
        this.lineRepository = lineRepository;
        this.accountingService = accountingService;
        this.saleRepository = saleRepository;
        this.branchRepository = branchRepository;
        this.customerRepository = customerRepository;
        this.issuedDocumentRepository = issuedDocumentRepository;
    }

    // --- comandos de entrada (los arma el controller desde el DTO) ---

    public record LineCommand(String accountCode, String glosa, BigDecimal debe, BigDecimal haber,
                              String docTipoSunat, String docSerieNumero,
                              String terceroDocTipo, String terceroDocNum, String terceroNombre) {
    }

    public record ManualEntryCommand(UUID companyId, LocalDate entryDate, String subdiario,
                                     String glosa, String moneda, List<LineCommand> lines) {
    }

    @Transactional
    public JournalEntry createManual(UUID tenantId, ManualEntryCommand cmd) {
        if (cmd.companyId() == null) {
            throw new BadRequestException("Falta la empresa (companyId).");
        }
        if (cmd.lines() == null || cmd.lines().size() < 2) {
            throw new BadRequestException("El asiento debe tener al menos dos lineas.");
        }
        AccountingConfig cfg = accountingService.getConfigOrDefault(tenantId, cmd.companyId());
        String subdiario = notBlank(cmd.subdiario()) ? cmd.subdiario() : cfg.getSubdiarioDiario();
        LocalDate date = cmd.entryDate() != null ? cmd.entryDate() : LocalDate.now(LIMA);

        JournalEntry entry = new JournalEntry();
        entry.setTenantId(tenantId);
        entry.setCompanyId(cmd.companyId());
        entry.setEntryDate(date);
        entry.setSubdiario(subdiario);
        entry.setGlosa(cmd.glosa());
        entry.setMoneda(notBlank(cmd.moneda()) ? cmd.moneda() : cfg.getMoneda());
        entry.setSource("MANUAL");
        entry.setCorrelativo(nextCorrelativo(tenantId, cmd.companyId(), subdiario));

        BigDecimal totalDebe = BigDecimal.ZERO;
        BigDecimal totalHaber = BigDecimal.ZERO;
        List<JournalEntryLine> lines = new ArrayList<>();
        int orden = 0;
        for (LineCommand lc : cmd.lines()) {
            if (lc.accountCode() == null || lc.accountCode().isBlank()) {
                throw new BadRequestException("Cada linea necesita una cuenta contable.");
            }
            BigDecimal debe = scale(lc.debe());
            BigDecimal haber = scale(lc.haber());
            totalDebe = totalDebe.add(debe);
            totalHaber = totalHaber.add(haber);
            lines.add(buildLine(tenantId, lc.accountCode(), lc.glosa(), debe, haber,
                    lc.docTipoSunat(), lc.docSerieNumero(), null,
                    lc.terceroDocTipo(), lc.terceroDocNum(), lc.terceroNombre(), orden++));
        }
        if (totalDebe.compareTo(totalHaber) != 0) {
            throw new BadRequestException("El asiento no cuadra: Debe (" + totalDebe
                    + ") debe ser igual a Haber (" + totalHaber + ").");
        }
        if (totalDebe.signum() == 0) {
            throw new BadRequestException("El asiento no puede tener importe cero.");
        }
        entry.setTotalDebe(totalDebe);
        entry.setTotalHaber(totalHaber);
        JournalEntry saved = entryRepository.save(entry);
        for (JournalEntryLine l : lines) {
            l.setJournalEntryId(saved.getId());
            lineRepository.save(l);
        }
        return saved;
    }

    /** Genera el asiento de venta (12 por cobrar / 40 IGV / 70 ventas) desde una venta del POS. */
    @Transactional
    public JournalEntry generateFromSale(UUID tenantId, UUID saleId) {
        if (entryRepository.existsByTenantIdAndSourceAndSourceId(tenantId, "SALE", saleId)) {
            throw new ConflictException("Esta venta ya tiene un asiento contable generado.");
        }
        Sale sale = saleRepository.findByIdAndTenantId(saleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada."));
        Branch branch = branchRepository.findByIdAndTenantId(sale.getBranchId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        UUID companyId = branch.getCompanyId();
        AccountingConfig cfg = accountingService.getConfigOrDefault(tenantId, companyId);

        // Preferimos el comprobante fiscal emitido (tiene base/IGV/serie); si no, calculamos.
        IssuedDocument fiscal = issuedDocumentRepository.findBySaleIdAndTenantId(saleId, tenantId).stream()
                .filter(d -> d.getDocumentType() != null && d.getDocumentType().isFiscal())
                .findFirst().orElse(null);

        BigDecimal total;
        BigDecimal subtotal;
        BigDecimal igv;
        String docTipoSunat;
        String serieNumero;
        LocalDate docFecha;
        if (fiscal != null) {
            total = scale(fiscal.getTotal());
            subtotal = scale(fiscal.getSubtotal());
            igv = scale(fiscal.getIgv());
            docTipoSunat = tabla10(fiscal.getDocumentType());
            serieNumero = fiscal.getFullNumber();
            docFecha = fiscal.getIssuedAt() != null ? fiscal.getIssuedAt().toLocalDate() : LocalDate.now(LIMA);
        } else {
            total = scale(sale.getTotal());
            subtotal = total.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
            igv = total.subtract(subtotal);
            docTipoSunat = "03"; // boleta por defecto
            serieNumero = null;
            docFecha = LocalDate.now(LIMA);
        }

        Customer customer = sale.getCustomerId() == null ? null
                : customerRepository.findByIdAndTenantId(sale.getCustomerId(), tenantId).orElse(null);
        String terceroTipo = customer != null ? tabla6(customer.getDocType()) : null;
        String terceroNum = customer != null ? customer.getDocNumber() : null;
        String terceroNombre = customer != null ? customer.getName() : "CLIENTES VARIOS";
        String glosa = "Venta " + (serieNumero != null ? serieNumero : sale.getId().toString().substring(0, 8));

        JournalEntry entry = new JournalEntry();
        entry.setTenantId(tenantId);
        entry.setCompanyId(companyId);
        entry.setEntryDate(docFecha);
        entry.setSubdiario(cfg.getSubdiarioVentas());
        entry.setGlosa(glosa);
        entry.setMoneda(cfg.getMoneda());
        entry.setSource("SALE");
        entry.setSourceId(saleId);
        entry.setCorrelativo(nextCorrelativo(tenantId, companyId, cfg.getSubdiarioVentas()));
        entry.setTotalDebe(total);
        entry.setTotalHaber(igv.add(subtotal));
        JournalEntry saved = entryRepository.save(entry);

        List<JournalEntryLine> lines = new ArrayList<>();
        lines.add(buildLine(tenantId, cfg.getCuentaPorCobrar(), glosa, total, BigDecimal.ZERO,
                docTipoSunat, serieNumero, docFecha, terceroTipo, terceroNum, terceroNombre, 0));
        lines.add(buildLine(tenantId, cfg.getCuentaIgv(), "IGV " + (serieNumero != null ? serieNumero : ""),
                BigDecimal.ZERO, igv, docTipoSunat, serieNumero, docFecha, terceroTipo, terceroNum, terceroNombre, 1));
        lines.add(buildLine(tenantId, cfg.getCuentaVentas(), glosa, BigDecimal.ZERO, subtotal,
                docTipoSunat, serieNumero, docFecha, terceroTipo, terceroNum, terceroNombre, 2));
        for (JournalEntryLine l : lines) {
            l.setJournalEntryId(saved.getId());
            lineRepository.save(l);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> list(UUID tenantId, UUID companyId, LocalDate from, LocalDate to) {
        return entryRepository
                .findByTenantIdAndCompanyIdAndEntryDateBetweenOrderByEntryDateAscCreatedAtAsc(
                        tenantId, companyId, from, to);
    }

    @Transactional(readOnly = true)
    public JournalEntry get(UUID tenantId, UUID id) {
        return entryRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Asiento no encontrado."));
    }

    @Transactional(readOnly = true)
    public List<JournalEntryLine> lines(UUID journalEntryId) {
        return lineRepository.findByJournalEntryIdOrderByOrdenAsc(journalEntryId);
    }

    // --- helpers ---

    private JournalEntryLine buildLine(UUID tenantId, String accountCode, String glosa, BigDecimal debe,
                                       BigDecimal haber, String docTipoSunat, String docSerieNumero,
                                       LocalDate docFecha, String terceroTipo, String terceroNum,
                                       String terceroNombre, int orden) {
        JournalEntryLine l = new JournalEntryLine();
        l.setTenantId(tenantId);
        l.setAccountCode(accountCode.trim());
        l.setGlosa(glosa);
        l.setDebe(scale(debe));
        l.setHaber(scale(haber));
        l.setDocTipoSunat(docTipoSunat);
        l.setDocSerieNumero(docSerieNumero);
        l.setDocFecha(docFecha);
        l.setTerceroDocTipo(terceroTipo);
        l.setTerceroDocNum(terceroNum);
        l.setTerceroNombre(terceroNombre);
        l.setOrden(orden);
        return l;
    }

    private String nextCorrelativo(UUID tenantId, UUID companyId, String subdiario) {
        long n = entryRepository.countByTenantIdAndCompanyIdAndSubdiario(tenantId, companyId, subdiario) + 1;
        return "%06d".formatted(n);
    }

    private static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** Tipo de comprobante SUNAT (tabla 10). */
    private static String tabla10(DocumentType t) {
        if (t == null) return "";
        return switch (t) {
            case INVOICE -> "01";
            case SALE_RECEIPT -> "03";
            case CREDIT_NOTE -> "07";
            case DEBIT_NOTE -> "08";
            default -> "";
        };
    }

    /** Tipo de documento de identidad SUNAT (tabla 6). */
    private static String tabla6(String docType) {
        if (docType == null) return null;
        return switch (docType.toUpperCase()) {
            case "RUC" -> "6";
            case "DNI" -> "1";
            case "CE" -> "4";
            case "PASSPORT", "PAS" -> "7";
            default -> "0";
        };
    }
}
