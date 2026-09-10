package com.sumaup360.erp.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Supplier;
import com.sumaup360.erp.repository.SupplierRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Proveedores por empresa del tenant. */
@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CompanyRepository companyRepository;

    public SupplierService(SupplierRepository supplierRepository,
                           CompanyRepository companyRepository) {
        this.supplierRepository = supplierRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public Supplier create(UUID tenantId, UUID companyId, String name, String ruc,
                           String phone, String email, String contactName,
                           String address, String notes) {
        requireCompany(tenantId, companyId);
        Supplier s = new Supplier();
        s.setTenantId(tenantId);
        s.setCompanyId(companyId);
        s.setName(name);
        s.setRuc(ruc);
        s.setPhone(phone);
        s.setEmail(email);
        s.setContactName(contactName);
        s.setAddress(address);
        s.setNotes(notes);
        return supplierRepository.save(s);
    }

    /** Edicion parcial: solo cambia lo que llega no nulo. */
    @Transactional
    public Supplier update(UUID id, UUID tenantId, String name, String ruc, String phone,
                           String email, String contactName, String address, String notes,
                           Boolean active) {
        Supplier s = get(id, tenantId);
        if (name != null && !name.isBlank()) s.setName(name);
        if (ruc != null) s.setRuc(ruc.isBlank() ? null : ruc);
        if (phone != null) s.setPhone(phone.isBlank() ? null : phone);
        if (email != null) s.setEmail(email.isBlank() ? null : email);
        if (contactName != null) s.setContactName(contactName.isBlank() ? null : contactName);
        if (address != null) s.setAddress(address.isBlank() ? null : address);
        if (notes != null) s.setNotes(notes.isBlank() ? null : notes);
        if (active != null) s.setActive(active);
        return supplierRepository.save(s);
    }

    @Transactional(readOnly = true)
    public List<Supplier> list(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        return supplierRepository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Transactional(readOnly = true)
    public Supplier get(UUID id, UUID tenantId) {
        return supplierRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado."));
    }

    private void requireCompany(UUID tenantId, UUID companyId) {
        if (companyId == null) {
            throw new BadRequestException("Falta la empresa (companyId).");
        }
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }
}
