package com.sumaup360.erp.lodging.dto;

import com.sumaup360.erp.lodging.domain.Room;
import com.sumaup360.erp.lodging.domain.RoomType;
import com.sumaup360.erp.lodging.domain.Stay;
import com.sumaup360.erp.lodging.domain.StayCharge;
import com.sumaup360.erp.lodging.domain.StayGuest;
import com.sumaup360.erp.lodging.enums.RentalMode;
import com.sumaup360.erp.lodging.enums.RoomStatus;
import com.sumaup360.erp.lodging.enums.StayStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public final class LodgingDtos {

    private LodgingDtos() {
    }

    // ------------------------------------------------------------------ tipos

    public record CreateRoomTypeRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 80) String name,
            @Positive Integer capacity,
            @NotNull @PositiveOrZero BigDecimal ratePerNight,
            @PositiveOrZero BigDecimal ratePerHour,
            @Size(max = 300) String description
    ) {
    }

    public record UpdateRoomTypeRequest(
            @Size(max = 80) String name,
            @Positive Integer capacity,
            @PositiveOrZero BigDecimal ratePerNight,
            @PositiveOrZero BigDecimal ratePerHour,
            @Size(max = 300) String description,
            Boolean isActive
    ) {
    }

    public record RoomTypeResponse(UUID id, String name, int capacity, BigDecimal ratePerNight,
                                   BigDecimal ratePerHour, String description, boolean isActive) {
        public static RoomTypeResponse from(RoomType t) {
            return new RoomTypeResponse(t.getId(), t.getName(), t.getCapacity(),
                    t.getRatePerNight(), t.getRatePerHour(), t.getDescription(), t.isActive());
        }
    }

    // ------------------------------------------------------------ habitaciones

    public record CreateRoomRequest(
            @NotNull UUID companyId,
            @NotNull UUID branchId,
            @NotNull UUID roomTypeId,
            @NotBlank @Size(max = 20) String number,
            @Size(max = 20) String floor,
            @Size(max = 300) String notes
    ) {
    }

    public record UpdateRoomRequest(
            UUID roomTypeId,
            @Size(max = 20) String number,
            @Size(max = 20) String floor,
            @Size(max = 300) String notes,
            Boolean isActive
    ) {
    }

    public record UpdateRoomStatusRequest(@NotNull RoomStatus status) {
    }

    public record RoomResponse(UUID id, UUID branchId, UUID roomTypeId, String roomTypeName,
                               BigDecimal ratePerNight, BigDecimal ratePerHour, int capacity,
                               String number, String floor,
                               RoomStatus status, String notes, boolean isActive) {
        public static RoomResponse from(Room r, RoomType type) {
            return new RoomResponse(r.getId(), r.getBranchId(), r.getRoomTypeId(),
                    type != null ? type.getName() : null,
                    type != null ? type.getRatePerNight() : null,
                    type != null ? type.getRatePerHour() : null,
                    type != null ? type.getCapacity() : 0,
                    r.getNumber(), r.getFloor(), r.getStatus(), r.getNotes(), r.isActive());
        }
    }

    /** Habitacion del rack: estado + estadia actual (si esta ocupada) + llegada de hoy. */
    public record RackRoomResponse(RoomResponse room, StaySummary currentStay, StaySummary arrivingStay) {
    }

    public record StaySummary(UUID id, String ticketCode, UUID customerId, String customerName,
                              LocalDate checkInDate, LocalDate checkOutDate, StayStatus status,
                              int guestsCount, RentalMode rentalMode, Integer hours,
                              OffsetDateTime checkedInAt) {
        public static StaySummary from(Stay s, String customerName) {
            return new StaySummary(s.getId(), s.getTicketCode(), s.getCustomerId(), customerName,
                    s.getCheckInDate(), s.getCheckOutDate(), s.getStatus(), s.getGuestsCount(),
                    s.getRentalMode(), s.getHours(), s.getCheckedInAt());
        }
    }

    // ---------------------------------------------------------------- estadias

    /**
     * NIGHTLY: exige fechas (salida exclusiva). HOURLY: walk-in de recepcion —
     * ignora fechas (hoy), exige hours y hace check-in inmediato. checkInNow
     * tambien sirve para NIGHTLY que entra en este momento. ratePerNight es la
     * tarifa pactada por unidad (noche u hora); si falta se toma del tipo.
     */
    public record CreateStayRequest(
            @NotNull UUID branchId,
            @NotNull UUID roomId,
            @NotNull UUID customerId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            RentalMode rentalMode,
            @Positive Integer hours,
            Boolean checkInNow,
            @PositiveOrZero BigDecimal ratePerNight,
            @Positive Integer guestsCount,
            @Valid List<GuestRequest> guests,
            @Size(max = 500) String notes
    ) {
    }

    public record UpdateStayRequest(
            UUID roomId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            @Positive Integer hours,
            @PositiveOrZero BigDecimal ratePerNight,
            @Positive Integer guestsCount,
            @Size(max = 500) String notes
    ) {
    }

    public record GuestRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 10) String docType,
            @NotBlank @Size(max = 20) String docNumber,
            @Size(max = 60) String nationality
    ) {
    }

    public record CheckInRequest(@Valid List<GuestRequest> guests) {
    }

    public record AddChargeRequest(
            UUID productId,
            @Size(max = 160) String description,
            @NotNull @Positive BigDecimal quantity,
            @PositiveOrZero BigDecimal unitPrice
    ) {
    }

    public record CheckOutRequest(String paymentMethod) {
    }

    /** Solo CANCELED o NO_SHOW; el resto de estados los fijan las acciones. */
    public record UpdateStayStatusRequest(@NotNull StayStatus status, @Size(max = 300) String reason) {
    }

    public record GuestResponse(UUID id, String fullName, String docType, String docNumber,
                                String nationality) {
        public static GuestResponse from(StayGuest g) {
            return new GuestResponse(g.getId(), g.getFullName(), g.getDocType(), g.getDocNumber(),
                    g.getNationality());
        }
    }

    public record ChargeResponse(UUID id, UUID productId, String description, BigDecimal quantity,
                                 BigDecimal unitPrice, BigDecimal lineTotal, OffsetDateTime createdAt) {
        public static ChargeResponse from(StayCharge c) {
            return new ChargeResponse(c.getId(), c.getProductId(), c.getDescription(),
                    c.getQuantity(), c.getUnitPrice(),
                    c.getUnitPrice().multiply(c.getQuantity()), c.getCreatedAt());
        }
    }

    public record StayResponse(UUID id, UUID branchId, UUID roomId, String roomNumber,
                               UUID customerId, String customerName,
                               LocalDate checkInDate, LocalDate checkOutDate, long nights,
                               RentalMode rentalMode, Integer hours,
                               StayStatus status, BigDecimal ratePerNight, int guestsCount,
                               OffsetDateTime checkedInAt, OffsetDateTime checkedOutAt,
                               UUID saleId, String ticketCode, String source, String notes,
                               BigDecimal chargesTotal,
                               List<GuestResponse> guests, List<ChargeResponse> charges) {
        public static StayResponse from(Stay s, String roomNumber, String customerName,
                                        BigDecimal chargesTotal,
                                        List<GuestResponse> guests, List<ChargeResponse> charges) {
            long nights = ChronoUnit.DAYS.between(s.getCheckInDate(), s.getCheckOutDate());
            return new StayResponse(s.getId(), s.getBranchId(), s.getRoomId(), roomNumber,
                    s.getCustomerId(), customerName, s.getCheckInDate(), s.getCheckOutDate(),
                    nights, s.getRentalMode(), s.getHours(),
                    s.getStatus(), s.getRatePerNight(), s.getGuestsCount(),
                    s.getCheckedInAt(), s.getCheckedOutAt(), s.getSaleId(), s.getTicketCode(),
                    s.getSource(), s.getNotes(), chargesTotal, guests, charges);
        }
    }
}
