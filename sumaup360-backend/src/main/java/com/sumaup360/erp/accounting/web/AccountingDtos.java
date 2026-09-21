package com.sumaup360.erp.accounting.web;

import com.sumaup360.erp.accounting.model.Account;
import com.sumaup360.erp.accounting.model.AccountingConfig;
import com.sumaup360.erp.accounting.model.JournalEntry;
import com.sumaup360.erp.accounting.model.JournalEntryLine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** DTOs del modulo de contabilidad (plan de cuentas, config, asientos). */
public final class AccountingDtos {

    private AccountingDtos() {
    }

    public record AccountResponse(UUID id, String code, String name, Short accountClass, String nature, boolean global) {
        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getId(), a.getCode(), a.getName(), a.getAccountClass(),
                    a.getNature(), a.getCompanyId() == null);
        }
    }

    public record CreateAccountRequest(@NotNull UUID companyId, @NotBlank String code, @NotBlank String name,
                                       Short accountClass, String nature) {
    }

    public record ConfigRequest(@NotNull UUID companyId, String subdiarioVentas, String subdiarioCompras,
                                String subdiarioDiario, String cuentaPorCobrar, String cuentaVentas,
                                String cuentaVentasServ, String cuentaIgv, String cuentaCaja,
                                String moneda, String concarSeparator) {
    }

    public record ConfigResponse(UUID companyId, String subdiarioVentas, String subdiarioCompras,
                                 String subdiarioDiario, String cuentaPorCobrar, String cuentaVentas,
                                 String cuentaVentasServ, String cuentaIgv, String cuentaCaja,
                                 String moneda, String concarSeparator) {
        public static ConfigResponse from(UUID companyId, AccountingConfig c) {
            AccountingConfig cfg = c != null ? c : new AccountingConfig();
            return new ConfigResponse(companyId, cfg.getSubdiarioVentas(), cfg.getSubdiarioCompras(),
                    cfg.getSubdiarioDiario(), cfg.getCuentaPorCobrar(), cfg.getCuentaVentas(),
                    cfg.getCuentaVentasServ(), cfg.getCuentaIgv(), cfg.getCuentaCaja(),
                    cfg.getMoneda(), cfg.getConcarSeparator());
        }
    }

    public record LineRequest(@NotBlank String accountCode, String glosa, BigDecimal debe, BigDecimal haber,
                              String docTipoSunat, String docSerieNumero,
                              String terceroDocTipo, String terceroDocNum, String terceroNombre) {
    }

    public record ManualEntryRequest(@NotNull UUID companyId, LocalDate entryDate, String subdiario,
                                     String glosa, String moneda, @NotEmpty List<LineRequest> lines) {
    }

    public record EntryLineResponse(String accountCode, String glosa, BigDecimal debe, BigDecimal haber,
                                    String docTipoSunat, String docSerieNumero,
                                    String terceroDocTipo, String terceroDocNum, String terceroNombre) {
        public static EntryLineResponse from(JournalEntryLine l) {
            return new EntryLineResponse(l.getAccountCode(), l.getGlosa(), l.getDebe(), l.getHaber(),
                    l.getDocTipoSunat(), l.getDocSerieNumero(), l.getTerceroDocTipo(),
                    l.getTerceroDocNum(), l.getTerceroNombre());
        }
    }

    public record EntryResponse(UUID id, LocalDate entryDate, String subdiario, String correlativo,
                                String glosa, String moneda, String source,
                                BigDecimal totalDebe, BigDecimal totalHaber) {
        public static EntryResponse from(JournalEntry e) {
            return new EntryResponse(e.getId(), e.getEntryDate(), e.getSubdiario(), e.getCorrelativo(),
                    e.getGlosa(), e.getMoneda(), e.getSource(), e.getTotalDebe(), e.getTotalHaber());
        }
    }

    public record EntryDetailResponse(EntryResponse entry, List<EntryLineResponse> lines) {
    }
}
