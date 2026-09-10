package com.sumaup360.erp.prescription.service;

import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.prescription.domain.Prescription;
import com.sumaup360.erp.prescription.dto.PrescriptionDtos.CreatePrescriptionRequest;
import com.sumaup360.erp.prescription.dto.PrescriptionDtos.PrescriptionResponse;
import com.sumaup360.erp.prescription.repository.PrescriptionRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Registro de recetas medicas (modulo prescription, MVP). */
@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               CustomerRepository customerRepository,
                               BranchRepository branchRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public PrescriptionResponse create(UUID tenantId, CreatePrescriptionRequest req) {
        branchRepository.findByIdAndTenantId(req.branchId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        String customerName = null;
        if (req.customerId() != null) {
            customerName = customerRepository.findByIdAndTenantId(req.customerId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."))
                    .getName();
        }
        Prescription p = new Prescription();
        p.setTenantId(tenantId);
        p.setBranchId(req.branchId());
        p.setCustomerId(req.customerId());
        p.setSaleId(req.saleId());
        p.setDoctorName(req.doctorName().trim());
        p.setDoctorLicense(req.doctorLicense());
        p.setIssuedDate(req.issuedDate());
        p.setDiagnosis(req.diagnosis());
        p.setMedications(req.medications());
        p.setNotes(req.notes());
        return PrescriptionResponse.from(prescriptionRepository.save(p), customerName);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> list(UUID tenantId, UUID branchId) {
        List<Prescription> items = (branchId == null)
                ? prescriptionRepository.findByTenantIdOrderByIssuedDateDesc(tenantId)
                : prescriptionRepository.findByTenantIdAndBranchIdOrderByIssuedDateDesc(tenantId, branchId);
        Map<UUID, String> names = customerRepository.findAllById(
                        items.stream().map(Prescription::getCustomerId).filter(Objects::nonNull)
                                .distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        return items.stream()
                .map(p -> PrescriptionResponse.from(p,
                        p.getCustomerId() != null ? names.get(p.getCustomerId()) : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse get(UUID tenantId, UUID id) {
        Prescription p = prescriptionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada."));
        String name = p.getCustomerId() != null
                ? customerRepository.findByIdAndTenantId(p.getCustomerId(), tenantId)
                        .map(Customer::getName).orElse(null)
                : null;
        return PrescriptionResponse.from(p, name);
    }
}
