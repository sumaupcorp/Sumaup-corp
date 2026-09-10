package com.sumaup360.erp.lodging.controller;

import com.sumaup360.erp.lodging.dto.LodgingDtos.CreateRoomRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.CreateRoomTypeRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.RackRoomResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.RoomResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.RoomTypeResponse;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateRoomRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateRoomStatusRequest;
import com.sumaup360.erp.lodging.dto.LodgingDtos.UpdateRoomTypeRequest;
import com.sumaup360.erp.lodging.service.RoomService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Tipos de habitacion, habitaciones y rack (modulo lodging). Multi-tenant + RBAC. */
@RestController
@RequestMapping("/api/v1/erp")
@Tag(name = "Hospedaje - Habitaciones", description = "Tipos, habitaciones, rack y disponibilidad")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ------------------------------------------------------------------ tipos

    @GetMapping("/room-types")
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Tipos de habitacion de la empresa (requiere lodging:read)")
    public List<RoomTypeResponse> listRoomTypes(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.listRoomTypes(tenantId, companyId);
    }

    @PostMapping("/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Crea un tipo de habitacion (requiere lodging:manage)")
    public RoomTypeResponse createRoomType(@Valid @RequestBody CreateRoomTypeRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.createRoomType(tenantId, req);
    }

    @PutMapping("/room-types/{id}")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Edita un tipo de habitacion (requiere lodging:manage)")
    public RoomTypeResponse updateRoomType(@PathVariable UUID id,
                                           @Valid @RequestBody UpdateRoomTypeRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.updateRoomType(tenantId, id, req);
    }

    // ------------------------------------------------------------ habitaciones

    @GetMapping("/rooms")
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Habitaciones de la empresa o sucursal (requiere lodging:read)")
    public List<RoomResponse> listRooms(@RequestParam UUID companyId,
                                        @RequestParam(required = false) UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.listRooms(tenantId, companyId, branchId);
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Crea una habitacion (requiere lodging:manage)")
    public RoomResponse createRoom(@Valid @RequestBody CreateRoomRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.createRoom(tenantId, req);
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Edita una habitacion (requiere lodging:manage)")
    public RoomResponse updateRoom(@PathVariable UUID id, @Valid @RequestBody UpdateRoomRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.updateRoom(tenantId, id, req);
    }

    @PatchMapping("/rooms/{id}/status")
    @PreAuthorize("hasAuthority('lodging:manage')")
    @Operation(summary = "Cambia el estado de una habitacion: limpieza, mantenimiento, disponible (requiere lodging:manage)")
    public RoomResponse updateRoomStatus(@PathVariable UUID id,
                                         @Valid @RequestBody UpdateRoomStatusRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.updateRoomStatus(tenantId, id, req.status());
    }

    // -------------------------------------------------------- rack y disponibles

    @GetMapping("/rooms/rack")
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Rack de la sucursal: habitaciones con estadia actual y llegada del dia (requiere lodging:read)")
    public List<RackRoomResponse> rack(@RequestParam UUID branchId,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.rack(tenantId, branchId, date);
    }

    @GetMapping("/rooms/availability")
    @PreAuthorize("hasAuthority('lodging:read')")
    @Operation(summary = "Habitaciones libres en el rango [from, to) (requiere lodging:read)")
    public List<RoomResponse> availability(@RequestParam UUID branchId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return roomService.availability(tenantId, branchId, from, to);
    }
}
