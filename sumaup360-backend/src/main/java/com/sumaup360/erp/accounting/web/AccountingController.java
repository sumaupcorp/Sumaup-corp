package com.sumaup360.erp.accounting.web;

import com.sumaup360.erp.accounting.model.AccountingConfig;
import com.sumaup360.erp.accounting.model.JournalEntry;
import com.sumaup360.erp.accounting.service.AccountingService;
import com.sumaup360.erp.accounting.service.ConcarExportService;
import com.sumaup360.erp.accounting.service.JournalEntryService;
import com.sumaup360.erp.accounting.service.JournalEntryService.LineCommand;
import com.sumaup360.erp.accounting.service.JournalEntryService.ManualEntryCommand;
import com.sumaup360.erp.accounting.web.AccountingDtos.AccountResponse;
import com.sumaup360.erp.accounting.web.AccountingDtos.ConfigRequest;
import com.sumaup360.erp.accounting.web.AccountingDtos.ConfigResponse;
import com.sumaup360.erp.accounting.web.AccountingDtos.CreateAccountRequest;
import com.sumaup360.erp.accounting.web.AccountingDtos.EntryDetailResponse;
import com.sumaup360.erp.accounting.web.AccountingDtos.EntryLineResponse;
import com.sumaup360.erp.accounting.web.AccountingDtos.EntryResponse;
import com.sumaup360.erp.accounting.web.AccountingDtos.ManualEntryRequest;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Contabilidad del tenant: plan de cuentas, configuracion, asientos y export a CONCAR. */
@RestController
@RequestMapping("/api/v1/erp/accounting")
@Tag(name = "Contabilidad", description = "Plan de cuentas, asientos contables y exportacion a CONCAR")
public class AccountingController {

    private final AccountingService accountingService;
    private final JournalEntryService journalEntryService;
    private final ConcarExportService concarExportService;

    public AccountingController(AccountingService accountingService,
                               JournalEntryService journalEntryService,
                               ConcarExportService concarExportService) {
        this.accountingService = accountingService;
        this.journalEntryService = journalEntryService;
        this.concarExportService = concarExportService;
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('accounting:read')")
    @Operation(summary = "Plan de cuentas de la empresa (PCGE global + propias) (requiere accounting:read)")
    public List<AccountResponse> accounts(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return accountingService.chart(tenantId, companyId).stream().map(AccountResponse::from).toList();
    }

    @PostMapping("/accounts")
    @PreAuthorize("hasAuthority('accounting:manage')")
    @Operation(summary = "Crea una cuenta contable propia de la empresa (requiere accounting:manage)")
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return AccountResponse.from(accountingService.createAccount(tenantId, req.companyId(),
                req.code(), req.name(), req.accountClass(), req.nature()));
    }

    @GetMapping("/config")
    @PreAuthorize("hasAuthority('accounting:read')")
    @Operation(summary = "Configuracion contable de la empresa (requiere accounting:read)")
    public ConfigResponse getConfig(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return ConfigResponse.from(companyId, accountingService.getConfig(tenantId, companyId));
    }

    @PutMapping("/config")
    @PreAuthorize("hasAuthority('accounting:manage')")
    @Operation(summary = "Guarda la configuracion contable (cuentas por defecto, subdiarios, formato CONCAR) (requiere accounting:manage)")
    public ConfigResponse saveConfig(@Valid @RequestBody ConfigRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        AccountingConfig in = new AccountingConfig();
        in.setSubdiarioVentas(req.subdiarioVentas());
        in.setSubdiarioCompras(req.subdiarioCompras());
        in.setSubdiarioDiario(req.subdiarioDiario());
        in.setCuentaPorCobrar(req.cuentaPorCobrar());
        in.setCuentaVentas(req.cuentaVentas());
        in.setCuentaVentasServ(req.cuentaVentasServ());
        in.setCuentaIgv(req.cuentaIgv());
        in.setCuentaCaja(req.cuentaCaja());
        in.setMoneda(req.moneda());
        in.setConcarSeparator(req.concarSeparator());
        return ConfigResponse.from(req.companyId(), accountingService.saveConfig(tenantId, req.companyId(), in));
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAuthority('accounting:read')")
    @Operation(summary = "Asientos contables de un periodo (requiere accounting:read)")
    public List<EntryResponse> entries(@RequestParam UUID companyId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return journalEntryService.list(tenantId, companyId, from, to).stream()
                .map(EntryResponse::from).toList();
    }

    @GetMapping("/entries/{id}")
    @PreAuthorize("hasAuthority('accounting:read')")
    @Operation(summary = "Detalle de un asiento con sus lineas (requiere accounting:read)")
    public EntryDetailResponse entry(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        JournalEntry e = journalEntryService.get(tenantId, id);
        List<EntryLineResponse> lines = journalEntryService.lines(e.getId()).stream()
                .map(EntryLineResponse::from).toList();
        return new EntryDetailResponse(EntryResponse.from(e), lines);
    }

    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('accounting:manage')")
    @Operation(summary = "Crea un asiento contable manual (Debe = Haber) (requiere accounting:manage)")
    public EntryResponse createEntry(@Valid @RequestBody ManualEntryRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        List<LineCommand> lines = req.lines().stream()
                .map(l -> new LineCommand(l.accountCode(), l.glosa(), l.debe(), l.haber(),
                        l.docTipoSunat(), l.docSerieNumero(), l.terceroDocTipo(), l.terceroDocNum(),
                        l.terceroNombre()))
                .toList();
        ManualEntryCommand cmd = new ManualEntryCommand(req.companyId(), req.entryDate(), req.subdiario(),
                req.glosa(), req.moneda(), lines);
        return EntryResponse.from(journalEntryService.createManual(tenantId, cmd));
    }

    @PostMapping("/entries/from-sale/{saleId}")
    @PreAuthorize("hasAuthority('accounting:manage')")
    @Operation(summary = "Genera el asiento contable de una venta (12/40/70) (requiere accounting:manage)")
    public EntryResponse fromSale(@PathVariable UUID saleId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return EntryResponse.from(journalEntryService.generateFromSale(tenantId, saleId));
    }

    @GetMapping("/concar/export")
    @PreAuthorize("hasAuthority('accounting:export')")
    @Operation(summary = "Exporta los asientos del periodo al formato de importacion de CONCAR (requiere accounting:export)")
    public ResponseEntity<byte[]> exportConcar(@RequestParam UUID companyId,
                                               @RequestParam int year,
                                               @RequestParam int month,
                                               @RequestParam(required = false, defaultValue = "txt") String format) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        ConcarExportService.ExportFile file = concarExportService.export(tenantId, companyId, year, month, format);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.filename()).build());
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.parseMediaType(file.contentType() + "; charset=UTF-8"))
                .body(file.content());
    }
}
