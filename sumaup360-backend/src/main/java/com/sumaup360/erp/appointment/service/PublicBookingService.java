package com.sumaup360.erp.appointment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.appointment.domain.Appointment;
import com.sumaup360.erp.appointment.domain.BookingPage;
import com.sumaup360.erp.appointment.dto.BookingDtos.BranchOption;
import com.sumaup360.erp.appointment.dto.BookingDtos.PublicBookingCreated;
import com.sumaup360.erp.appointment.dto.BookingDtos.PublicBookingInfo;
import com.sumaup360.erp.appointment.dto.BookingDtos.PublicBookingRequest;
import com.sumaup360.erp.appointment.enums.AppointmentStatus;
import com.sumaup360.erp.appointment.repository.AppointmentRepository;
import com.sumaup360.erp.appointment.repository.BookingPageRepository;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.patient.domain.Patient;
import com.sumaup360.erp.patient.repository.PatientRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reserva de citas desde la web publica (QR). No expone datos del negocio mas alla de lo
 * configurado y crea la cita como REQUESTED para que el negocio la confirme en su panel.
 */
@Service
public class PublicBookingService {

    private static final Logger log = LoggerFactory.getLogger(PublicBookingService.class);

    private final BookingPageRepository bookingPageRepository;
    private final BookingPageService bookingPageService;
    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final PatientRepository patientRepository;
    private final BranchRepository branchRepository;
    private final ObjectMapper mapper;

    public PublicBookingService(BookingPageRepository bookingPageRepository,
                                BookingPageService bookingPageService,
                                AppointmentRepository appointmentRepository,
                                CustomerRepository customerRepository,
                                PatientRepository patientRepository,
                                BranchRepository branchRepository,
                                ObjectMapper mapper) {
        this.bookingPageRepository = bookingPageRepository;
        this.bookingPageService = bookingPageService;
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.patientRepository = patientRepository;
        this.branchRepository = branchRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PublicBookingInfo info(String token) {
        BookingPage page = requireEnabledPage(token);
        List<BranchOption> branches = branchRepository.findByCompanyId(page.getCompanyId()).stream()
                .map(b -> new BranchOption(b.getId(), b.getName()))
                .toList();
        return new PublicBookingInfo(true, page.getTitle(), page.getLogoUrl(), page.getWelcomeText(),
                bookingPageService.parseFormConfig(page), branches);
    }

    @Transactional
    public PublicBookingCreated createRequest(String token, PublicBookingRequest req) {
        BookingPage page = requireEnabledPage(token);
        Branch branch = branchRepository.findByIdAndTenantId(req.branchId(), page.getTenantId())
                .orElseThrow(() -> new BadRequestException("La sucursal elegida no es valida."));
        if (!branch.getCompanyId().equals(page.getCompanyId())) {
            throw new BadRequestException("La sucursal elegida no es valida.");
        }
        validateRequest(req);

        UUID tenantId = page.getTenantId();
        Customer customer = customerRepository
                .findFirstByTenantIdAndCompanyIdAndPhone(tenantId, page.getCompanyId(), req.phone().trim())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setTenantId(tenantId);
                    c.setCompanyId(page.getCompanyId());
                    c.setName(req.name().trim());
                    c.setPhone(req.phone().trim());
                    return customerRepository.save(c);
                });

        // Anti-basura: un mismo cliente no acumula reservas pendientes en la misma sucursal.
        if (appointmentRepository.existsByTenantIdAndBranchIdAndCustomerIdAndStatus(
                tenantId, branch.getId(), customer.getId(), AppointmentStatus.REQUESTED)) {
            throw new BadRequestException(
                    "Ya tienes una reserva pendiente con este negocio. Te contactaran para confirmarla.");
        }

        UUID patientId = null;
        if (req.patientName() != null && !req.patientName().isBlank()) {
            String patientName = req.patientName().trim();
            patientId = patientRepository
                    .findByTenantIdAndCustomerIdOrderByNameAsc(tenantId, customer.getId()).stream()
                    .filter(p -> p.getName().equalsIgnoreCase(patientName))
                    .findFirst()
                    .orElseGet(() -> {
                        Patient p = new Patient();
                        p.setTenantId(tenantId);
                        p.setCustomerId(customer.getId());
                        p.setName(patientName);
                        return patientRepository.save(p);
                    })
                    .getId();
        }

        Appointment a = new Appointment();
        a.setTenantId(tenantId);
        a.setBranchId(branch.getId());
        a.setCustomerId(customer.getId());
        a.setPatientId(patientId);
        a.setScheduledAt(req.preferredAt());
        a.setStatus(AppointmentStatus.REQUESTED);
        a.setSource("ONLINE");
        a.setReason(reasonFrom(req, page));
        a.setFormData(answersJson(req, page));
        a.setTicketCode(TicketCodes.next(appointmentRepository, tenantId));
        appointmentRepository.save(a);
        log.info("Reserva online creada para tenant {} (sucursal {})", tenantId, branch.getId());
        return new PublicBookingCreated(true,
                "Recibimos tu solicitud de cita. El negocio te contactara para confirmarla.",
                a.getTicketCode(), page.getTitle(), branch.getName(),
                customer.getName(), a.getScheduledAt());
    }

    /** Validaciones anti-basura que Bean Validation no cubre. */
    private void validateRequest(PublicBookingRequest req) {
        String digits = req.phone().replaceAll("\\D", "");
        if (digits.length() < 6 || digits.length() > 15) {
            throw new BadRequestException("Ingresa un telefono valido.");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (req.preferredAt().isBefore(now.minusHours(1))) {
            throw new BadRequestException("Elige una fecha y hora futura.");
        }
        if (req.preferredAt().isAfter(now.plusDays(90))) {
            throw new BadRequestException("La fecha debe estar dentro de los proximos 90 dias.");
        }
    }

    private String reasonFrom(PublicBookingRequest req, BookingPage page) {
        if (req.answers() != null && allowedFieldKeys(page).contains("motivo")) {
            String motivo = req.answers().get("motivo");
            if (motivo != null && !motivo.isBlank()) {
                return motivo.length() > 160 ? motivo.substring(0, 160) : motivo;
            }
        }
        return "Reserva online";
    }

    /**
     * Serializa las respuestas aceptando SOLO las claves configuradas en el formulario de la
     * pagina (con tope de longitud por valor): el endpoint es publico y sin esto cualquiera
     * podria inyectar claves arbitrarias o valores gigantes en form_data.
     */
    private String answersJson(PublicBookingRequest req, BookingPage page) {
        try {
            var node = mapper.createObjectNode();
            node.put("nombre", req.name());
            node.put("telefono", req.phone());
            if (req.patientName() != null && !req.patientName().isBlank()) {
                node.put("mascota", req.patientName());
            }
            if (req.answers() != null) {
                Set<String> allowed = allowedFieldKeys(page);
                req.answers().forEach((k, v) -> {
                    if (k != null && v != null && allowed.contains(k) && !node.has(k)) {
                        node.put(k, v.length() > 300 ? v.substring(0, 300) : v);
                    }
                });
            }
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private Set<String> allowedFieldKeys(BookingPage page) {
        Set<String> keys = new HashSet<>();
        bookingPageService.parseFormConfig(page).forEach(field -> {
            var key = field.get("key");
            if (key != null && key.isTextual()) keys.add(key.asText());
        });
        return keys;
    }

    private BookingPage requireEnabledPage(String token) {
        return bookingPageRepository.findByToken(token)
                .filter(BookingPage::isEnabled)
                .orElseThrow(() -> new ResourceNotFoundException("Esta pagina de reserva no esta disponible."));
    }
}
