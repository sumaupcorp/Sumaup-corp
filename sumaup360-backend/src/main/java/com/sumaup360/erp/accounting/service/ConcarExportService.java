package com.sumaup360.erp.accounting.service;

import com.sumaup360.erp.accounting.model.AccountingConfig;
import com.sumaup360.erp.accounting.model.JournalEntry;
import com.sumaup360.erp.accounting.model.JournalEntryLine;
import com.sumaup360.erp.accounting.repository.JournalEntryLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Exporta los asientos de un periodo al formato de importacion de CONCAR (cabecera+detalle
 * denormalizado, una fila por linea). El separador es configurable por empresa porque el
 * layout exacto varia por version de CONCAR / plantilla del estudio contable.
 */
@Service
public class ConcarExportService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] HEADERS = {
            "SubDiario", "NumeroComprobante", "Fecha", "Moneda", "Glosa", "CuentaContable",
            "TipoDocumento", "NumeroDocumento", "FechaDocumento", "Debe", "Haber",
            "TerceroTipoDoc", "TerceroNumDoc", "TerceroNombre"
    };

    private final JournalEntryService journalEntryService;
    private final AccountingService accountingService;
    private final JournalEntryLineRepository lineRepository;

    public ConcarExportService(JournalEntryService journalEntryService,
                               AccountingService accountingService,
                               JournalEntryLineRepository lineRepository) {
        this.journalEntryService = journalEntryService;
        this.accountingService = accountingService;
        this.lineRepository = lineRepository;
    }

    public record ExportFile(byte[] content, String filename, String contentType) {
    }

    @Transactional(readOnly = true)
    public ExportFile export(UUID tenantId, UUID companyId, int year, int month, String format) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        AccountingConfig cfg = accountingService.getConfigOrDefault(tenantId, companyId);
        boolean csv = "csv".equalsIgnoreCase(format);
        String sep = csv ? "," : cfg.getConcarSeparator();

        List<JournalEntry> entries = journalEntryService.list(tenantId, companyId, from, to);
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(sep, HEADERS)).append("\r\n");
        for (JournalEntry e : entries) {
            for (JournalEntryLine l : lineRepository.findByJournalEntryIdOrderByOrdenAsc(e.getId())) {
                String[] row = {
                        nz(e.getSubdiario()),
                        nz(e.getCorrelativo()),
                        e.getEntryDate() != null ? e.getEntryDate().format(D) : "",
                        nz(e.getMoneda()),
                        clean(e.getGlosa(), sep),
                        nz(l.getAccountCode()),
                        nz(l.getDocTipoSunat()),
                        nz(l.getDocSerieNumero()),
                        l.getDocFecha() != null ? l.getDocFecha().format(D) : "",
                        l.getDebe().toPlainString(),
                        l.getHaber().toPlainString(),
                        nz(l.getTerceroDocTipo()),
                        nz(l.getTerceroDocNum()),
                        clean(l.getTerceroNombre(), sep)
                };
                sb.append(String.join(sep, row)).append("\r\n");
            }
        }
        String ext = csv ? "csv" : "txt";
        String contentType = csv ? "text/csv" : "text/plain";
        String filename = "concar_%d%02d.%s".formatted(year, month, ext);
        return new ExportFile(sb.toString().getBytes(StandardCharsets.UTF_8), filename, contentType);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Evita romper el archivo si un texto trae el separador o saltos de linea. */
    private static String clean(String s, String sep) {
        if (s == null) return "";
        return s.replace(sep, " ").replace("\r", " ").replace("\n", " ").trim();
    }
}
