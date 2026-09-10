package com.sumaup360.erp.lodging.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.lodging.domain.Room;
import com.sumaup360.erp.lodging.domain.RoomType;
import com.sumaup360.erp.lodging.domain.Stay;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CreateRoomRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CreateRoomTypeRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.RackRoomResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.RoomResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.RoomTypeResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.StaySummary;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateRoomRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateRoomTypeRequest;
import com.sumaup360.erp.lodging.enums.RoomStatus;
import com.sumaup360.erp.lodging.enums.StayStatus;
import com.sumaup360.erp.lodging.repository.RoomRepository;
import com.sumaup360.erp.lodging.repository.RoomTypeRepository;
import com.sumaup360.erp.lodging.repository.StayRepository;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Tipos de habitacion, habitaciones, rack y disponibilidad (modulo lodging). */
@Service
public class RoomService {

    private static final List<StayStatus> LIVE_STATUSES =
            List.of(StayStatus.RESERVED, StayStatus.CHECKED_IN);

    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final StayRepository stayRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;

    public RoomService(RoomTypeRepository roomTypeRepository,
                       RoomRepository roomRepository,
                       StayRepository stayRepository,
                       CustomerRepository customerRepository,
                       BranchRepository branchRepository,
                       CompanyRepository companyRepository) {
        this.roomTypeRepository = roomTypeRepository;
        this.roomRepository = roomRepository;
        this.stayRepository = stayRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
    }

    // ------------------------------------------------------------------ tipos

    @Transactional
    public RoomTypeResponse createRoomType(UUID tenantId, CreateRoomTypeRequest req) {
        requireCompany(tenantId, req.companyId());
        RoomType t = new RoomType();
        t.setTenantId(tenantId);
        t.setCompanyId(req.companyId());
        t.setName(req.name().trim());
        t.setCapacity(req.capacity() != null ? req.capacity() : 2);
        t.setRatePerNight(req.ratePerNight());
        t.setRatePerHour(req.ratePerHour());
        t.setDescription(req.description());
        return RoomTypeResponse.from(roomTypeRepository.save(t));
    }

    @Transactional
    public RoomTypeResponse updateRoomType(UUID tenantId, UUID id, UpdateRoomTypeRequest req) {
        RoomType t = roomTypeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitacion no encontrado."));
        if (req.name() != null && !req.name().isBlank()) t.setName(req.name().trim());
        if (req.capacity() != null) t.setCapacity(req.capacity());
        if (req.ratePerNight() != null) t.setRatePerNight(req.ratePerNight());
        if (req.ratePerHour() != null) {
            // 0 = quitar la tarifa por hora (deja de alquilarse por horas).
            t.setRatePerHour(req.ratePerHour().signum() == 0 ? null : req.ratePerHour());
        }
        if (req.description() != null) t.setDescription(req.description());
        if (req.isActive() != null) t.setActive(req.isActive());
        return RoomTypeResponse.from(roomTypeRepository.save(t));
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> listRoomTypes(UUID tenantId, UUID companyId) {
        requireCompany(tenantId, companyId);
        return roomTypeRepository.findByTenantIdAndCompanyIdOrderByNameAsc(tenantId, companyId)
                .stream().map(RoomTypeResponse::from).toList();
    }

    // ------------------------------------------------------------ habitaciones

    @Transactional
    public RoomResponse createRoom(UUID tenantId, CreateRoomRequest req) {
        requireCompany(tenantId, req.companyId());
        Branch branch = requireBranch(tenantId, req.branchId());
        if (!branch.getCompanyId().equals(req.companyId())) {
            throw new BadRequestException("La sucursal no pertenece a esa empresa.");
        }
        RoomType type = requireRoomType(tenantId, req.companyId(), req.roomTypeId());
        String number = req.number().trim();
        if (roomRepository.existsByTenantIdAndBranchIdAndNumberIgnoreCase(tenantId, req.branchId(), number)) {
            throw new BadRequestException("Ya existe una habitacion con ese numero en la sucursal.");
        }
        Room r = new Room();
        r.setTenantId(tenantId);
        r.setCompanyId(req.companyId());
        r.setBranchId(req.branchId());
        r.setRoomTypeId(type.getId());
        r.setNumber(number);
        r.setFloor(req.floor());
        r.setNotes(req.notes());
        return RoomResponse.from(roomRepository.save(r), type);
    }

    @Transactional
    public RoomResponse updateRoom(UUID tenantId, UUID id, UpdateRoomRequest req) {
        Room r = requireRoom(tenantId, id);
        if (req.roomTypeId() != null) {
            requireRoomType(tenantId, r.getCompanyId(), req.roomTypeId());
            r.setRoomTypeId(req.roomTypeId());
        }
        if (req.number() != null && !req.number().isBlank()) {
            String number = req.number().trim();
            if (!number.equalsIgnoreCase(r.getNumber())
                    && roomRepository.existsByTenantIdAndBranchIdAndNumberIgnoreCase(
                            tenantId, r.getBranchId(), number)) {
                throw new BadRequestException("Ya existe una habitacion con ese numero en la sucursal.");
            }
            r.setNumber(number);
        }
        if (req.floor() != null) r.setFloor(req.floor());
        if (req.notes() != null) r.setNotes(req.notes());
        if (req.isActive() != null) r.setActive(req.isActive());
        Room saved = roomRepository.save(r);
        return RoomResponse.from(saved, roomTypeRepository.findById(saved.getRoomTypeId()).orElse(null));
    }

    /** Cambio manual de estado. OCCUPIED solo via check-in; ocupada no se toca. */
    @Transactional
    public RoomResponse updateRoomStatus(UUID tenantId, UUID id, RoomStatus status) {
        Room r = requireRoom(tenantId, id);
        if (status == RoomStatus.OCCUPIED) {
            throw new BadRequestException("La habitacion se ocupa con el check-in, no manualmente.");
        }
        if (stayRepository.existsByTenantIdAndRoomIdAndStatus(tenantId, r.getId(), StayStatus.CHECKED_IN)) {
            throw new BadRequestException("La habitacion tiene un huesped: haz el check-out primero.");
        }
        r.setStatus(status);
        Room saved = roomRepository.save(r);
        return RoomResponse.from(saved, roomTypeRepository.findById(saved.getRoomTypeId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(UUID tenantId, UUID companyId, UUID branchId) {
        requireCompany(tenantId, companyId);
        List<Room> rooms = branchId != null
                ? roomRepository.findByTenantIdAndBranchIdOrderByNumberAsc(tenantId, branchId)
                : roomRepository.findByTenantIdAndCompanyIdOrderByNumberAsc(tenantId, companyId);
        Map<UUID, RoomType> types = typesOf(rooms);
        return rooms.stream().map(r -> RoomResponse.from(r, types.get(r.getRoomTypeId()))).toList();
    }

    // -------------------------------------------------------------------- rack

    /** Rack de la sucursal: cada habitacion con su estadia actual y la llegada del dia. */
    @Transactional(readOnly = true)
    public List<RackRoomResponse> rack(UUID tenantId, UUID branchId, LocalDate date) {
        requireBranch(tenantId, branchId);
        LocalDate day = date != null ? date : LocalDate.now(ZoneId.of("America/Lima"));
        List<Room> rooms = roomRepository.findByTenantIdAndBranchIdOrderByNumberAsc(tenantId, branchId)
                .stream().filter(Room::isActive).toList();
        List<Stay> live = stayRepository.findByTenantIdAndBranchIdAndStatusIn(
                tenantId, branchId, LIVE_STATUSES);
        Map<UUID, String> customers = customerNames(live);
        Map<UUID, RoomType> types = typesOf(rooms);

        Map<UUID, Stay> current = live.stream()
                .filter(s -> s.getStatus() == StayStatus.CHECKED_IN)
                .collect(Collectors.toMap(Stay::getRoomId, Function.identity(), (a, b) -> a));
        Map<UUID, Stay> arriving = live.stream()
                .filter(s -> s.getStatus() == StayStatus.RESERVED && !s.getCheckInDate().isAfter(day))
                .collect(Collectors.toMap(Stay::getRoomId, Function.identity(),
                        (a, b) -> a.getCheckInDate().isBefore(b.getCheckInDate()) ? a : b));

        return rooms.stream().map(r -> {
            Stay cur = current.get(r.getId());
            Stay arr = arriving.get(r.getId());
            return new RackRoomResponse(
                    RoomResponse.from(r, types.get(r.getRoomTypeId())),
                    cur != null ? StaySummary.from(cur, customers.get(cur.getCustomerId())) : null,
                    arr != null ? StaySummary.from(arr, customers.get(arr.getCustomerId())) : null);
        }).toList();
    }

    /** Habitaciones activas de la sucursal sin estadia viva que cruce [from, to). */
    @Transactional(readOnly = true)
    public List<RoomResponse> availability(UUID tenantId, UUID branchId, LocalDate from, LocalDate to) {
        requireBranch(tenantId, branchId);
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException("El rango de fechas es invalido.");
        }
        var busy = stayRepository
                .findByTenantIdAndBranchIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                        tenantId, branchId, LIVE_STATUSES, to, from)
                .stream().map(Stay::getRoomId).collect(Collectors.toSet());
        List<Room> rooms = roomRepository.findByTenantIdAndBranchIdOrderByNumberAsc(tenantId, branchId)
                .stream().filter(r -> r.isActive() && !busy.contains(r.getId())).toList();
        Map<UUID, RoomType> types = typesOf(rooms);
        return rooms.stream().map(r -> RoomResponse.from(r, types.get(r.getRoomTypeId()))).toList();
    }

    // ---------------------------------------------------------------- soporte

    private Map<UUID, RoomType> typesOf(List<Room> rooms) {
        return roomTypeRepository.findAllById(
                        rooms.stream().map(Room::getRoomTypeId).distinct().toList())
                .stream().collect(Collectors.toMap(RoomType::getId, Function.identity()));
    }

    private Map<UUID, String> customerNames(List<Stay> stays) {
        return customerRepository.findAllById(
                        stays.stream().map(Stay::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
    }

    private RoomType requireRoomType(UUID tenantId, UUID companyId, UUID roomTypeId) {
        RoomType type = roomTypeRepository.findByIdAndTenantId(roomTypeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de habitacion no encontrado."));
        if (!type.getCompanyId().equals(companyId)) {
            throw new BadRequestException("El tipo de habitacion no pertenece a esa empresa.");
        }
        return type;
    }

    private Room requireRoom(UUID tenantId, UUID id) {
        return roomRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Habitacion no encontrada."));
    }

    private Branch requireBranch(UUID tenantId, UUID branchId) {
        return branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }

    private void requireCompany(UUID tenantId, UUID companyId) {
        companyRepository.findByIdAndTenantId(companyId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa no encontrada."));
    }
}
