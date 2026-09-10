package com.sumaup360.erp.appointment.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.appointment.domain.Appointment;
import com.sumaup360.erp.appointment.dto.AppointmentDtos.AppointmentResponse;
import com.sumaup360.erp.appointment.dto.AppointmentDtos.CreateAppointmentRequest;
import com.sumaup360.erp.appointment.dto.AppointmentDtos.UpdateAppointmentRequest;
import com.sumaup360.erp.appointment.enums.AppointmentStatus;
import com.sumaup360.erp.appointment.repository.AppointmentRepository;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.patient.domain.Patient;
import com.sumaup360.erp.patient.repository.PatientRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Citas por sucursal (modulo appointments). */
@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              CustomerRepository customerRepository,
                              PatientRepository patientRepository,
                              BranchRepository branchRepository) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.patientRepository = patientRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public AppointmentResponse create(UUID tenantId, CreateAppointmentRequest req) {
        requireBranch(tenantId, req.branchId());
        Customer customer = customerRepository.findByIdAndTenantId(req.customerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
        String patientName = null;
        if (req.patientId() != null) {
            Patient p = patientRepository.findByIdAndTenantId(req.patientId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
            patientName = p.getName();
        }
        Appointment a = new Appointment();
        a.setTenantId(tenantId);
        a.setBranchId(req.branchId());
        a.setCustomerId(req.customerId());
        a.setPatientId(req.patientId());
        a.setReason(req.reason());
        a.setScheduledAt(req.scheduledAt());
        a.setDurationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 30);
        a.setStatus(AppointmentStatus.SCHEDULED);
        a.setAssignedTo(req.assignedTo());
        a.setNotes(req.notes());
        a.setSource("INTERNAL");
        a.setTicketCode(TicketCodes.next(appointmentRepository, tenantId));
        return AppointmentResponse.from(appointmentRepository.save(a), customer.getName(), patientName);
    }

    /** Busqueda rapida por codigo de ticket (el que recibe el cliente al reservar). */
    @Transactional(readOnly = true)
    public AppointmentResponse getByTicket(UUID tenantId, String code) {
        Appointment a = appointmentRepository
                .findByTenantIdAndTicketCode(tenantId, code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna cita con ese codigo."));
        return withNames(List.of(a)).get(0);
    }

    /** Agenda: citas entre dos fechas (por defecto: desde hoy, 30 dias). */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(UUID tenantId, UUID branchId,
                                          OffsetDateTime from, OffsetDateTime to) {
        OffsetDateTime f = from != null ? from : OffsetDateTime.now().minusDays(1);
        OffsetDateTime t = to != null ? to : OffsetDateTime.now().plusDays(30);
        if (t.isBefore(f)) {
            throw new BadRequestException("El rango de fechas es invalido.");
        }
        List<Appointment> items = (branchId == null)
                ? appointmentRepository.findByTenantIdAndScheduledAtBetweenOrderByScheduledAtAsc(tenantId, f, t)
                : appointmentRepository.findByTenantIdAndBranchIdAndScheduledAtBetweenOrderByScheduledAtAsc(
                        tenantId, branchId, f, t);
        return withNames(items);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(UUID tenantId, UUID id) {
        Appointment a = requireAppointment(tenantId, id);
        return withNames(List.of(a)).get(0);
    }

    @Transactional
    public AppointmentResponse update(UUID tenantId, UUID id, UpdateAppointmentRequest req) {
        Appointment a = requireAppointment(tenantId, id);
        if (req.scheduledAt() != null) a.setScheduledAt(req.scheduledAt());
        if (req.durationMinutes() != null) a.setDurationMinutes(req.durationMinutes());
        if (req.reason() != null) a.setReason(req.reason());
        if (req.patientId() != null) {
            patientRepository.findByIdAndTenantId(req.patientId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado."));
            a.setPatientId(req.patientId());
        }
        if (req.assignedTo() != null) a.setAssignedTo(req.assignedTo());
        if (req.notes() != null) a.setNotes(req.notes());
        return withNames(List.of(appointmentRepository.save(a))).get(0);
    }

    @Transactional
    public AppointmentResponse updateStatus(UUID tenantId, UUID id, AppointmentStatus status) {
        Appointment a = requireAppointment(tenantId, id);
        a.setStatus(status);
        return withNames(List.of(appointmentRepository.save(a))).get(0);
    }

    private List<AppointmentResponse> withNames(List<Appointment> items) {
        Map<UUID, String> customers = customerRepository.findAllById(
                        items.stream().map(Appointment::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        Map<UUID, String> patients = patientRepository.findAllById(
                        items.stream().map(Appointment::getPatientId).filter(java.util.Objects::nonNull)
                                .distinct().toList())
                .stream().collect(Collectors.toMap(Patient::getId, Patient::getName, (a, b) -> a));
        return items.stream()
                .map(a -> AppointmentResponse.from(a, customers.get(a.getCustomerId()),
                        a.getPatientId() != null ? patients.get(a.getPatientId()) : null))
                .toList();
    }

    private Appointment requireAppointment(UUID tenantId, UUID id) {
        return appointmentRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada."));
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }
}
