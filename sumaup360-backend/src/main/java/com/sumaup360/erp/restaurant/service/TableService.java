package com.sumaup360.erp.restaurant.service;

import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.restaurant.domain.RestaurantTable;
import com.sumaup360.erp.restaurant.enums.TableStatus;
import com.sumaup360.erp.restaurant.repository.RestaurantTableRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Mesas del restaurante (por sucursal del tenant del contexto). */
@Service
public class TableService {

    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;

    public TableService(RestaurantTableRepository tableRepository, BranchRepository branchRepository) {
        this.tableRepository = tableRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public RestaurantTable create(UUID tenantId, UUID branchId, String name, String zone, Integer capacity) {
        requireBranch(tenantId, branchId);
        tableRepository.findByTenantIdAndBranchId(tenantId, branchId).stream()
                .filter(t -> t.getName().equalsIgnoreCase(name))
                .findAny()
                .ifPresent(t -> { throw new ConflictException("Ya existe una mesa '" + name + "'."); });
        RestaurantTable t = new RestaurantTable();
        t.setTenantId(tenantId);
        t.setBranchId(branchId);
        t.setName(name);
        t.setZone(zone);
        t.setCapacity(capacity != null ? capacity : 4);
        t.setStatus(TableStatus.FREE);
        t.setActive(true);
        return tableRepository.save(t);
    }

    @Transactional(readOnly = true)
    public List<RestaurantTable> list(UUID tenantId, UUID branchId) {
        requireBranch(tenantId, branchId);
        return tableRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public RestaurantTable get(UUID id, UUID tenantId) {
        return tableRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada."));
    }

    @Transactional
    public RestaurantTable updateStatus(UUID id, UUID tenantId, TableStatus status) {
        RestaurantTable t = get(id, tenantId);
        t.setStatus(status);
        return tableRepository.save(t);
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }
}
