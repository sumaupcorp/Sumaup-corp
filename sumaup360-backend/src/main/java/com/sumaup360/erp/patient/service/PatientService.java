package com.sumaup360.erp.patient.service;

import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.patient.domain.Patient;
import com.sumaup360.erp.patient.dto.PatientDtos.CreatePatientRequest;
import com.sumaup360.erp.patient.dto.PatientDtos.PatientResponse;
import com.sumaup360.erp.patient.dto.PatientDtos.UpdatePatientRequest;
import com.sumaup360.erp.patient.repository.PatientRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Pacientes / mascotas del negocio (modulo patients). */
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final CustomerRepository customerRepository;

    public PatientService(PatientRepository patientRepository, CustomerRepository customerRepository) {
        this.patientRepository = patientRepository;
        this.customerRepository = customerRepository;
    }

    @Transactional
    public PatientResponse create(UUID tenantId, CreatePatientRequest req) {
        Customer owner = requireCustomer(tenantId, req.customerId());
        Patient p = new Patient();
        p.setTenantId(tenantId);
        p.setCustomerId(req.customerId());
        p.setName(req.name().trim());
        p.setSpecies(req.species());
        p.setBreed(req.breed());
        p.setSex(req.sex());
        p.setBirthDate(req.birthDate());
        p.setWeightKg(req.weightKg());
        p.setNotes(req.notes());
        p.setPhotoUrl(req.photoUrl());
        return PatientResponse.from(patientRepository.save(p), owner.getName());
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> list(UUID tenantId, UUID customerId) {
        List<Patient> patients = (customerId == null)
                ? patientRepository.findByTenantIdOrderByNameAsc(tenantId)
                : patientRepository.findByTenantIdAndCustomerIdOrderByNameAsc(tenantId, customerId);
        Map<UUID, String> owners = customerRepository.findAllById(
                        patients.stream().map(Patient::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        return patients.stream()
                .map(p -> PatientResponse.from(p, owners.get(p.getCustomerId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse get(UUID tenantId, UUID id) {
        Patient p = requirePatient(tenantId, id);
        String owner = customerRepository.findByIdAndTenantId(p.getCustomerId(), tenantId)
                .map(Customer::getName).orElse(null);
        return PatientResponse.from(p, owner);
    }

    @Transactional
    public PatientResponse update(UUID tenantId, UUID id, UpdatePatientRequest req) {
        Patient p = requirePatient(tenantId, id);
        if (req.name() != null && !req.name().isBlank()) p.setName(req.name().trim());
        if (req.species() != null) p.setSpecies(req.species());
        if (req.breed() != null) p.setBreed(req.breed());
        if (req.sex() != null) p.setSex(req.sex());
        if (req.birthDate() != null) p.setBirthDate(req.birthDate());
        if (req.weightKg() != null) p.setWeightKg(req.weightKg());
        if (req.notes() != null) p.setNotes(req.notes());
        // Cadena vacia = quitar la foto; null = no tocarla.
        if (req.photoUrl() != null) p.setPhotoUrl(req.photoUrl().isBlank() ? null : req.photoUrl());
        if (req.active() != null) p.setActive(req.active());
        String owner = customerRepository.findByIdAndTenantId(p.getCustomerId(), tenantId)
                .map(Customer::getName).orElse(null);
        return PatientResponse.from(patientRepository.save(p), owner);
    }

    private Patient requirePatient(UUID tenantId, UUID id) {
        return patientRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
    }

    private Customer requireCustomer(UUID tenantId, UUID customerId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
    }
}
