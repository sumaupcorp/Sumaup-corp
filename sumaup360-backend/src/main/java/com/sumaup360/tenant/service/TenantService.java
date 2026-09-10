package com.sumaup360.tenant.service;

import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.tenant.domain.Tenant;
import com.sumaup360.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Gestion de tenants (operacion cross-tenant; tipica del Backoffice/staff). */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RoleService roleService;

    public TenantService(TenantRepository tenantRepository, RoleService roleService) {
        this.tenantRepository = tenantRepository;
        this.roleService = roleService;
    }

    @Transactional
    public Tenant create(String code, String name) {
        tenantRepository.findByCode(code).ifPresent(t -> {
            throw new ConflictException("Ya existe un tenant con el codigo '" + code + "'.");
        });
        Tenant tenant = new Tenant();
        tenant.setCode(code);
        tenant.setName(name);
        tenant.setStatus("ACTIVE");
        Tenant saved = tenantRepository.save(tenant);
        // Cada tenant nace con su rol administrador del negocio.
        roleService.createTenantAdminRole(saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Tenant> list() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Tenant get(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado."));
    }
}
