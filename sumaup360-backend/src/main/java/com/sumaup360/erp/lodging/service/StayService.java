package com.sumaup360.erp.lodging.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.lodging.domain.Room;
import com.sumaup360.erp.lodging.domain.RoomType;
import com.sumaup360.erp.lodging.domain.Stay;
import com.sumaup360.erp.lodging.domain.StayCharge;
import com.sumaup360.erp.lodging.domain.StayGuest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.AddChargeRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.ChargeResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CheckInRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CheckOutRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CreateStayRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.GuestRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.GuestResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.StayResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateStayRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateStayStatusRequest;
import com.sumaup360.erp.lodging.enums.RentalMode;
import com.sumaup360.erp.lodging.enums.StayStatus;
import com.sumaup360.erp.lodging.repository.RoomRepository;
import com.sumaup360.erp.lodging.repository.RoomTypeRepository;
import com.sumaup360.erp.lodging.repository.StayChargeRepository;
import com.sumaup360.erp.lodging.repository.StayGuestRepository;
import com.sumaup360.erp.lodging.repository.StayRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.erp.service.SaleService;
import com.sumaup360.erp.service.SaleService.SaleLine;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Ciclo de vida de la estadia: reservar (sin solape), check-in con registro de
 * huespedes, cargos a la habitacion y check-out que liquida todo como venta POS.
 */
@Service
public class StayService {

    private static final List<StayStatus> LIVE_STATUSES =
            List.of(StayStatus.RESERVED, StayStatus.CHECKED_IN);
    private static final java.time.ZoneId LIMA = java.time.ZoneId.of("America/Lima");
    private static final String TICKET_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StayRepository stayRepository;
    private final StayGuestRepository stayGuestRepository;
    private final StayChargeRepository stayChargeRepository;
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final ProductRepository productRepository;
    private final SaleService saleService;

    public StayService(StayRepository stayRepository,
                       StayGuestRepository stayGuestRepository,
                       StayChargeRepository stayChargeRepository,
                       RoomRepository roomRepository,
                       RoomTypeRepository roomTypeRepository,
                       CustomerRepository customerRepository,
                       BranchRepository branchRepository,
                       CompanyRepository companyRepository,
                       ProductRepository productRepository,
                       SaleService saleService) {
        this.stayRepository = stayRepository;
        this.stayGuestRepository = stayGuestRepository;
        this.stayChargeRepository = stayChargeRepository;
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.productRepository = productRepository;
        this.saleService = saleService;
    }

    /**
     * Crea una reserva (NIGHTLY) o un alquiler walk-in. HOURLY entra y sale el
     * mismo dia, no bloquea fechas futuras y hace check-in inmediato; NIGHTLY
     * con checkInNow tambien ocupa la habitacion al instante.
     */
    @Transactional
    public StayResponse create(UUID tenantId, CreateStayRequest req) {
        branchRepository.findByIdAndTenantId(req.branchId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        Room room = requireRoom(tenantId, req.roomId());
        if (!room.getBranchId().equals(req.branchId())) {
            throw new BadRequestException("La habitacion no pertenece a esa sucursal.");
        }
        if (!room.isActive()) {
            throw new BadRequestException("La habitacion esta desactivada.");
        }
        Customer customer = requireCustomer(tenantId, req.customerId());
        RoomType type = roomTypeRepository.findByIdAndTenantId(room.getRoomTypeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitacion no encontrado."));
        int guests = req.guestsCount() != null ? req.guestsCount()
                : (req.guests() != null && !req.guests().isEmpty() ? req.guests().size() : 1);
        if (guests > type.getCapacity()) {
            throw new BadRequestException("La habitacion admite hasta "
                    + type.getCapacity() + " huespedes.");
        }

        RentalMode mode = req.rentalMode() != null ? req.rentalMode() : RentalMode.NIGHTLY;
        boolean checkInNow = mode == RentalMode.HOURLY
                || Boolean.TRUE.equals(req.checkInNow());

        Stay s = new Stay();
        s.setTenantId(tenantId);
        s.setCompanyId(room.getCompanyId());
        s.setBranchId(room.getBranchId());
        s.setRoomId(room.getId());
        s.setCustomerId(customer.getId());
        s.setRentalMode(mode);
        s.setGuestsCount(guests);
        s.setNotes(req.notes());
        s.setSource("INTERNAL");
        s.setTicketCode(nextTicketCode(tenantId));

        if (mode == RentalMode.HOURLY) {
            if (req.hours() == null || req.hours() < 1) {
                throw new BadRequestException("Indica cuantas horas se alquila la habitacion.");
            }
            BigDecimal rate = req.ratePerNight() != null ? req.ratePerNight() : type.getRatePerHour();
            if (rate == null) {
                throw new BadRequestException(
                        "El tipo " + type.getName() + " no tiene tarifa por hora configurada.");
            }
            LocalDate today = LocalDate.now(LIMA);
            s.setCheckInDate(today);
            s.setCheckOutDate(today);   // mismo dia: no bloquea el solape nocturno
            s.setHours(req.hours());
            s.setRatePerNight(rate);    // tarifa pactada POR HORA (unidad = rentalMode)
        } else {
            validateDates(req.checkInDate(), req.checkOutDate());
            assertNoOverlap(tenantId, room.getId(), req.checkInDate(), req.checkOutDate(), null);
            s.setCheckInDate(req.checkInDate());
            s.setCheckOutDate(req.checkOutDate());
            s.setRatePerNight(req.ratePerNight() != null ? req.ratePerNight() : type.getRatePerNight());
        }

        if (checkInNow) {
            if (room.getStatus() != com.sumaup360.erp.lodging.enums.RoomStatus.AVAILABLE) {
                throw new BadRequestException("La habitacion no esta disponible en este momento.");
            }
            s.setStatus(StayStatus.CHECKED_IN);
            s.setCheckedInAt(OffsetDateTime.now());
            room.setStatus(com.sumaup360.erp.lodging.enums.RoomStatus.OCCUPIED);
            roomRepository.save(room);
        }

        Stay saved = stayRepository.save(s);
        if (checkInNow && req.guests() != null && !req.guests().isEmpty()) {
            registerGuests(tenantId, saved, req.guests());
        }
        return toResponse(saved, room, customer.getName(), checkInNow);
    }

    /**
     * Reprogramar reservas pendientes (revalida solape). Ademas, una estadia por
     * horas con check-in activo puede EXTENDER sus horas (el cliente pide mas).
     */
    @Transactional
    public StayResponse update(UUID tenantId, UUID id, UpdateStayRequest req) {
        Stay s = requireStay(tenantId, id);
        if (s.getStatus() == StayStatus.CHECKED_IN && s.getRentalMode() == RentalMode.HOURLY) {
            if (req.hours() != null) s.setHours(req.hours());
            if (req.notes() != null) s.setNotes(req.notes());
            Room hourlyRoom = requireRoom(tenantId, s.getRoomId());
            return toResponse(stayRepository.save(s), hourlyRoom,
                    customerName(tenantId, s.getCustomerId()), true);
        }
        if (s.getStatus() != StayStatus.RESERVED) {
            throw new BadRequestException("Solo se puede modificar una reserva pendiente.");
        }
        Room room = requireRoom(tenantId, s.getRoomId());
        if (req.roomId() != null && !req.roomId().equals(s.getRoomId())) {
            Room target = requireRoom(tenantId, req.roomId());
            if (!target.getBranchId().equals(s.getBranchId())) {
                throw new BadRequestException("La habitacion nueva debe ser de la misma sucursal.");
            }
            if (!target.isActive()) {
                throw new BadRequestException("La habitacion esta desactivada.");
            }
            room = target;
            s.setRoomId(target.getId());
        }
        if (req.checkInDate() != null) s.setCheckInDate(req.checkInDate());
        if (req.checkOutDate() != null) s.setCheckOutDate(req.checkOutDate());
        validateDates(s.getCheckInDate(), s.getCheckOutDate());
        if (req.ratePerNight() != null) s.setRatePerNight(req.ratePerNight());
        if (req.guestsCount() != null) s.setGuestsCount(req.guestsCount());
        if (req.notes() != null) s.setNotes(req.notes());
        assertNoOverlap(tenantId, s.getRoomId(), s.getCheckInDate(), s.getCheckOutDate(), s.getId());
        return toResponse(stayRepository.save(s), room, customerName(tenantId, s.getCustomerId()), false);
    }

    /** Check-in: registra huespedes (ficha legal) y ocupa la habitacion. */
    @Transactional
    public StayResponse checkIn(UUID tenantId, UUID id, CheckInRequest req) {
        Stay s = requireStay(tenantId, id);
        if (s.getStatus() != StayStatus.RESERVED) {
            throw new BadRequestException("Solo se puede hacer check-in de una reserva pendiente.");
        }
        Room room = requireRoom(tenantId, s.getRoomId());
        if (stayRepository.existsByTenantIdAndRoomIdAndStatus(tenantId, room.getId(),
                StayStatus.CHECKED_IN)) {
            throw new BadRequestException("La habitacion aun esta ocupada: haz el check-out primero.");
        }
        if (req != null && req.guests() != null && !req.guests().isEmpty()) {
            registerGuests(tenantId, s, req.guests());
            s.setGuestsCount(req.guests().size());
        }
        s.setStatus(StayStatus.CHECKED_IN);
        s.setCheckedInAt(OffsetDateTime.now());
        room.setStatus(com.sumaup360.erp.lodging.enums.RoomStatus.OCCUPIED);
        roomRepository.save(room);
        return toResponse(stayRepository.save(s), room, customerName(tenantId, s.getCustomerId()), true);
    }

    /** Cargo a la habitacion durante la estadia (con producto o libre). */
    @Transactional
    public ChargeResponse addCharge(UUID tenantId, UUID stayId, AddChargeRequest req, UUID userId) {
        Stay s = requireStay(tenantId, stayId);
        if (s.getStatus() != StayStatus.CHECKED_IN) {
            throw new BadRequestException("Solo se puede cargar a una estadia con check-in.");
        }
        StayCharge c = new StayCharge();
        c.setTenantId(tenantId);
        c.setStayId(s.getId());
        c.setQuantity(req.quantity());
        c.setCreatedBy(userId);
        if (req.productId() != null) {
            Product p = productRepository.findByIdAndTenantId(req.productId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
            c.setProductId(p.getId());
            c.setDescription(req.description() != null && !req.description().isBlank()
                    ? req.description().trim() : p.getName());
            c.setUnitPrice(req.unitPrice() != null ? req.unitPrice() : p.getPrice());
        } else {
            if (req.description() == null || req.description().isBlank()) {
                throw new BadRequestException("El cargo sin producto necesita descripcion.");
            }
            if (req.unitPrice() == null) {
                throw new BadRequestException("El cargo sin producto necesita precio.");
            }
            c.setDescription(req.description().trim());
            c.setUnitPrice(req.unitPrice());
        }
        return ChargeResponse.from(stayChargeRepository.save(c));
    }

    @Transactional
    public void removeCharge(UUID tenantId, UUID stayId, UUID chargeId) {
        Stay s = requireStay(tenantId, stayId);
        if (s.getStatus() != StayStatus.CHECKED_IN) {
            throw new BadRequestException("Solo se pueden quitar cargos antes del check-out.");
        }
        StayCharge c = stayChargeRepository.findByIdAndTenantId(chargeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cargo no encontrado."));
        if (!c.getStayId().equals(s.getId())) {
            throw new BadRequestException("El cargo no pertenece a esa estadia.");
        }
        stayChargeRepository.delete(c);
    }

    /**
     * Check-out: noches + cargos se liquidan como venta POS normal (exige caja
     * abierta; el ticket, arqueo y facturacion siguen el flujo existente).
     */
    @Transactional
    public StayResponse checkOut(UUID tenantId, UUID id, CheckOutRequest req, UUID userId) {
        Stay s = requireStay(tenantId, id);
        if (s.getStatus() != StayStatus.CHECKED_IN) {
            throw new BadRequestException("Solo se puede hacer check-out de una estadia con check-in.");
        }
        Room room = requireRoom(tenantId, s.getRoomId());

        List<SaleLine> lines = new ArrayList<>();
        if (s.getRentalMode() == RentalMode.HOURLY) {
            long hours = s.getHours() != null && s.getHours() >= 1 ? s.getHours() : 1;
            lines.add(new SaleLine(null,
                    "Hospedaje hab. " + room.getNumber() + " (" + hours
                            + (hours == 1 ? " hora)" : " horas)"),
                    BigDecimal.valueOf(hours), s.getRatePerNight()));
        } else {
            long nights = Math.max(1, ChronoUnit.DAYS.between(s.getCheckInDate(), s.getCheckOutDate()));
            lines.add(new SaleLine(null,
                    "Hospedaje hab. " + room.getNumber() + " (" + nights
                            + (nights == 1 ? " noche)" : " noches)"),
                    BigDecimal.valueOf(nights), s.getRatePerNight()));
        }
        for (StayCharge c : stayChargeRepository
                .findByTenantIdAndStayIdOrderByCreatedAtAsc(tenantId, s.getId())) {
            lines.add(new SaleLine(c.getProductId(),
                    c.getProductId() == null ? c.getDescription() : null,
                    c.getQuantity(), c.getUnitPrice()));
        }
        Sale sale = saleService.createWithLines(tenantId, s.getBranchId(), s.getCustomerId(),
                lines, req != null ? req.paymentMethod() : null, userId);

        s.setSaleId(sale.getId());
        s.setStatus(StayStatus.CHECKED_OUT);
        s.setCheckedOutAt(OffsetDateTime.now());
        room.setStatus(com.sumaup360.erp.lodging.enums.RoomStatus.CLEANING);
        roomRepository.save(room);
        return toResponse(stayRepository.save(s), room, customerName(tenantId, s.getCustomerId()), true);
    }

    /** Cancelacion o no show: solo sobre reservas pendientes. */
    @Transactional
    public StayResponse updateStatus(UUID tenantId, UUID id, UpdateStayStatusRequest req) {
        if (req.status() != StayStatus.CANCELED && req.status() != StayStatus.NO_SHOW) {
            throw new BadRequestException("Ese estado lo fijan las acciones de check-in/check-out.");
        }
        Stay s = requireStay(tenantId, id);
        if (s.getStatus() != StayStatus.RESERVED) {
            throw new BadRequestException("Solo se puede cancelar una reserva pendiente.");
        }
        s.setStatus(req.status());
        if (req.reason() != null && !req.reason().isBlank()) {
            String prefix = s.getNotes() != null && !s.getNotes().isBlank() ? s.getNotes() + " | " : "";
            s.setNotes(limit(prefix + "Motivo: " + req.reason().trim(), 500));
        }
        Room room = requireRoom(tenantId, s.getRoomId());
        return toResponse(stayRepository.save(s), room, customerName(tenantId, s.getCustomerId()), false);
    }

    @Transactional(readOnly = true)
    public Page<StayResponse> search(UUID tenantId, UUID companyId, UUID branchId,
                                     StayStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "checkInDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));
        Page<Stay> stays;
        if (branchId != null) {
            branchRepository.findByIdAndTenantId(branchId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
            stays = status != null
                    ? stayRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status, pageable)
                    : stayRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable);
        } else {
            companyRepository.findByIdAndTenantId(companyId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada."));
            stays = status != null
                    ? stayRepository.findByTenantIdAndCompanyIdAndStatus(tenantId, companyId, status, pageable)
                    : stayRepository.findByTenantIdAndCompanyId(tenantId, companyId, pageable);
        }
        Map<UUID, String> customers = customerRepository.findAllById(
                        stays.getContent().stream().map(Stay::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        Map<UUID, String> rooms = roomRepository.findAllById(
                        stays.getContent().stream().map(Stay::getRoomId).distinct().toList())
                .stream().collect(Collectors.toMap(Room::getId, Room::getNumber, (a, b) -> a));
        return stays.map(s -> StayResponse.from(s, rooms.get(s.getRoomId()),
                customers.get(s.getCustomerId()), null, null, null));
    }

    @Transactional(readOnly = true)
    public StayResponse get(UUID tenantId, UUID id) {
        Stay s = requireStay(tenantId, id);
        Room room = requireRoom(tenantId, s.getRoomId());
        return toResponse(s, room, customerName(tenantId, s.getCustomerId()), true);
    }

    @Transactional(readOnly = true)
    public StayResponse getByTicket(UUID tenantId, String code) {
        Stay s = stayRepository.findByTenantIdAndTicketCode(tenantId, code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna reserva con ese codigo."));
        Room room = requireRoom(tenantId, s.getRoomId());
        return toResponse(s, room, customerName(tenantId, s.getCustomerId()), true);
    }

    // ---------------------------------------------------------------- soporte

    private StayResponse toResponse(Stay s, Room room, String customerName, boolean withDetails) {
        List<GuestResponse> guests = null;
        List<ChargeResponse> charges = null;
        BigDecimal chargesTotal = null;
        if (withDetails) {
            guests = stayGuestRepository
                    .findByTenantIdAndStayIdOrderByCreatedAtAsc(s.getTenantId(), s.getId())
                    .stream().map(GuestResponse::from).toList();
            charges = stayChargeRepository
                    .findByTenantIdAndStayIdOrderByCreatedAtAsc(s.getTenantId(), s.getId())
                    .stream().map(ChargeResponse::from).toList();
            chargesTotal = charges.stream().map(ChargeResponse::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return StayResponse.from(s, room.getNumber(), customerName, chargesTotal, guests, charges);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new BadRequestException("La fecha de salida debe ser posterior a la de entrada.");
        }
    }

    private void assertNoOverlap(UUID tenantId, UUID roomId, LocalDate checkIn,
                                 LocalDate checkOut, UUID excludeId) {
        long overlapping = stayRepository.countOverlapping(tenantId, roomId, LIVE_STATUSES,
                checkIn, checkOut, excludeId);
        if (overlapping > 0) {
            throw new BadRequestException("La habitacion ya tiene una reserva en esas fechas.");
        }
    }

    private String nextTicketCode(UUID tenantId) {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(6);
            for (int j = 0; j < 6; j++) {
                sb.append(TICKET_ALPHABET.charAt(RANDOM.nextInt(TICKET_ALPHABET.length())));
            }
            String code = sb.toString();
            if (!stayRepository.existsByTenantIdAndTicketCode(tenantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("No se pudo generar un codigo de ticket unico.");
    }

    /** Ficha legal de huespedes: reemplaza el registro previo de la estadia. */
    private void registerGuests(UUID tenantId, Stay s, List<GuestRequest> guests) {
        stayGuestRepository.deleteByTenantIdAndStayId(tenantId, s.getId());
        for (GuestRequest g : guests) {
            StayGuest guest = new StayGuest();
            guest.setTenantId(tenantId);
            guest.setStayId(s.getId());
            guest.setFullName(g.fullName().trim());
            guest.setDocType(g.docType() != null && !g.docType().isBlank()
                    ? g.docType().trim().toUpperCase() : "DNI");
            guest.setDocNumber(g.docNumber().trim());
            guest.setNationality(g.nationality() != null && !g.nationality().isBlank()
                    ? g.nationality().trim() : "Peru");
            stayGuestRepository.save(guest);
        }
    }

    private String customerName(UUID tenantId, UUID customerId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .map(Customer::getName).orElse(null);
    }

    private Stay requireStay(UUID tenantId, UUID id) {
        return stayRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada."));
    }

    private Room requireRoom(UUID tenantId, UUID id) {
        return roomRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Habitacion no encontrada."));
    }

    private Customer requireCustomer(UUID tenantId, UUID id) {
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
