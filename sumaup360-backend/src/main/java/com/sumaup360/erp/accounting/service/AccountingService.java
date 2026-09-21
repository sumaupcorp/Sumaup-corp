package com.sumaup360.erp.accounting.service;

import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.accounting.model.Account;
import com.sumaup360.erp.accounting.model.AccountingConfig;
import com.sumaup360.erp.accounting.repository.AccountRepository;
import com.sumaup360.erp.accounting.repository.AccountingConfigRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Plan de cuentas (global PCGE + propias) y configuracion contable por empresa. */
@Service
public class AccountingService {

    private final AccountRepository accountRepository;
    private final AccountingConfigRepository configRepository;
    private final CompanyRepository companyRepository;

    public AccountingService(AccountRepository accountRepository,
                             AccountingConfigRepository configRepository,
                             CompanyRepository companyRepository) {
        this.accountRepository = accountRepository;
        this.configRepository = configRepository;
        this.companyRepository = companyRepository;
    }

    /** Plan de cuentas visible para una empresa: plantilla global PCGE + cuentas propias. */
    @Transactional(readOnly = true)
    public List<Account> chart(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        List<Account> all = new ArrayList<>(accountRepository.findByTenantIdIsNullAndCompanyIdIsNull());
        all.addAll(accountRepository.findByTenantIdAndCompanyId(tenantId, companyId));
        all.sort((a, b) -> a.getCode().compareTo(b.getCode()));
        return all;
    }

    @Transactional
    public Account createAccount(UUID tenantId, UUID companyId, String code, String name,
                                 Short accountClass, String nature) {
        requireCompany(tenantId, companyId);
        Account a = new Account();
        a.setTenantId(tenantId);
        a.setCompanyId(companyId);
        a.setCode(code.trim());
        a.setName(name.trim());
        a.setAccountClass(accountClass);
        a.setNature(nature);
        a.setActive(true);
        return accountRepository.save(a);
    }

    @Transactional(readOnly = true)
    public AccountingConfig getConfig(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        return configRepository.findByTenantIdAndCompanyId(tenantId, companyId).orElse(null);
    }

    /** Config guardada, o una con valores por defecto (no persistida) para generar/exportar. */
    @Transactional(readOnly = true)
    public AccountingConfig getConfigOrDefault(UUID tenantId, UUID companyId) {
        return configRepository.findByTenantIdAndCompanyId(tenantId, companyId).orElseGet(() -> {
            AccountingConfig c = new AccountingConfig();
            c.setTenantId(tenantId);
            c.setCompanyId(companyId);
            return c;
        });
    }

    @Transactional
    public AccountingConfig saveConfig(UUID tenantId, UUID companyId, AccountingConfig in) {
        requireCompany(tenantId, companyId);
        AccountingConfig cfg = configRepository.findByTenantIdAndCompanyId(tenantId, companyId)
                .orElseGet(() -> {
                    AccountingConfig c = new AccountingConfig();
                    c.setTenantId(tenantId);
                    c.setCompanyId(companyId);
                    return c;
                });
        cfg.setSubdiarioVentas(orKeep(in.getSubdiarioVentas(), cfg.getSubdiarioVentas()));
        cfg.setSubdiarioCompras(orKeep(in.getSubdiarioCompras(), cfg.getSubdiarioCompras()));
        cfg.setSubdiarioDiario(orKeep(in.getSubdiarioDiario(), cfg.getSubdiarioDiario()));
        cfg.setCuentaPorCobrar(orKeep(in.getCuentaPorCobrar(), cfg.getCuentaPorCobrar()));
        cfg.setCuentaVentas(orKeep(in.getCuentaVentas(), cfg.getCuentaVentas()));
        cfg.setCuentaVentasServ(orKeep(in.getCuentaVentasServ(), cfg.getCuentaVentasServ()));
        cfg.setCuentaIgv(orKeep(in.getCuentaIgv(), cfg.getCuentaIgv()));
        cfg.setCuentaCaja(orKeep(in.getCuentaCaja(), cfg.getCuentaCaja()));
        cfg.setMoneda(orKeep(in.getMoneda(), cfg.getMoneda()));
        cfg.setConcarSeparator(orKeep(in.getConcarSeparator(), cfg.getConcarSeparator()));
        return configRepository.save(cfg);
    }

    private void requireCompany(UUID tenantId, UUID companyId) {
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }

    private static String orKeep(String value, String current) {
        return value != null && !value.isBlank() ? value.trim() : current;
    }
}
