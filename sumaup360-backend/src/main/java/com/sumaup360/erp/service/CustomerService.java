package com.sumaup360.erp.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.appointment.domain.Appointment;
import com.sumaup360.erp.appointment.enums.AppointmentStatus;
import com.sumaup360.erp.appointment.repository.AppointmentRepository;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.patient.domain.Patient;
import com.sumaup360.erp.patient.repository.PatientRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.erp.repository.SaleItemRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.erp.web.dto.CustomerDtos.AppointmentBrief;
import com.sumaup360.erp.web.dto.CustomerDtos.CustomerResponse;
import com.sumaup360.erp.web.dto.CustomerDtos.CustomerSummaryResponse;
import com.sumaup360.erp.web.dto.CustomerDtos.SaleBrief;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Clientes por empresa del tenant (los negocios del mismo dueño no comparten clientes). */
@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CompanyRepository companyRepository;
    private final AppointmentRepository appointmentRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PatientRepository patientRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CompanyRepository companyRepository,
                           AppointmentRepository appointmentRepository,
                           SaleRepository saleRepository,
                           SaleItemRepository saleItemRepository,
                           PatientRepository patientRepository) {
        this.customerRepository = customerRepository;
        this.companyRepository = companyRepository;
        this.appointmentRepository = appointmentRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional
    public Customer create(UUID tenantId, UUID companyId, String name, String docType,
                           String docNumber, String email, String phone) {
        requireCompany(tenantId, companyId);
        Customer c = new Customer();
        c.setTenantId(tenantId);
        c.setCompanyId(companyId);
        c.setName(name);
        c.setDocType(docType);
        c.setDocNumber(docNumber);
        c.setEmail(email);
        c.setPhone(phone);
        return customerRepository.save(c);
    }

    @Transactional(readOnly = true)
    public List<Customer> list(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        return customerRepository.findByTenantIdAndCompanyId(tenantId, companyId);
    }

    @Transactional(readOnly = true)
    public Customer get(UUID id, UUID tenantId) {
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
    }

    /**
     * Ficha del cliente: estadisticas de atencion (citas por estado, proximas, compras y
     * gasto total, ultima visita) + su historial reciente y sus mascotas. Una sola llamada.
     */
    @Transactional(readOnly = true)
    public CustomerSummaryResponse summary(UUID id, UUID tenantId) {
        Customer c = get(id, tenantId);

        List<Appointment> appointments =
                appointmentRepository.findByTenantIdAndCustomerIdOrderByScheduledAtDesc(tenantId, id);
        List<Sale> sales = saleRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, id).stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .toList();
        List<Patient> patients =
                patientRepository.findByTenantIdAndCustomerIdOrderByNameAsc(tenantId, id);
        Map<UUID, String> patientNames = patients.stream()
                .collect(Collectors.toMap(Patient::getId, Patient::getName, (a, b) -> a));

        OffsetDateTime now = OffsetDateTime.now();
        long attended = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();
        long canceled = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELED).count();
        long noShow = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW).count();
        long upcoming = appointments.stream()
                .filter(a -> a.getScheduledAt() != null && a.getScheduledAt().isAfter(now)
                        && a.getStatus() != AppointmentStatus.CANCELED
                        && a.getStatus() != AppointmentStatus.COMPLETED)
                .count();

        BigDecimal totalSpent = sales.stream()
                .map(Sale::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Ultima visita real: cita atendida mas reciente o ultima compra, lo que sea mayor.
        OffsetDateTime lastAttended = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(Appointment::getScheduledAt)
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo).orElse(null);
        OffsetDateTime lastSale = sales.stream()
                .map(Sale::getCreatedAt)
                .filter(java.util.Objects::nonNull)
                .max(OffsetDateTime::compareTo).orElse(null);
        OffsetDateTime lastVisit = lastAttended == null ? lastSale
                : lastSale == null ? lastAttended
                : lastAttended.isAfter(lastSale) ? lastAttended : lastSale;

        List<AppointmentBrief> recentAppointments = appointments.stream()
                .limit(10)
                .map(a -> new AppointmentBrief(a.getId(), a.getScheduledAt(),
                        a.getStatus().name(), a.getReason(),
                        a.getPatientId() != null ? patientNames.get(a.getPatientId()) : null))
                .toList();
        List<SaleBrief> recentSales = sales.stream()
                .limit(10)
                .map(s -> new SaleBrief(s.getId(), s.getCreatedAt(), s.getTotal(),
                        s.getPaymentMethod(), saleItemRepository.findBySaleId(s.getId()).size()))
                .toList();

        return new CustomerSummaryResponse(
                CustomerResponse.from(c),
                appointments.size(), attended, canceled, noShow, upcoming,
                sales.size(), totalSpent, lastVisit,
                patients.stream().map(Patient::getName).toList(),
                recentAppointments, recentSales);
    }

    private void requireCompany(UUID tenantId, UUID companyId) {
        if (companyId == null) {
            throw new BadRequestException("Falta la empresa (companyId).");
        }
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada en este tenant."));
    }
}
