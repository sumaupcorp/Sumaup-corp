package com.sumaup360.erp.appointment.controller;

import com.sumaup360.erp.appointment.dto.BookingDtos.BookingPageView;
import com.sumaup360.erp.appointment.dto.BookingDtos.UpdateBookingPageRequest;
import com.sumaup360.erp.appointment.service.BookingPageService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Configuracion de la pagina publica de reserva de citas (QR + formulario dinamico). */
@RestController
@RequestMapping("/api/v1/erp/booking-page")
@Tag(name = "Reserva online", description = "Configuracion del formulario publico de reserva de citas")
public class BookingPageController {

    private final BookingPageService bookingPageService;

    public BookingPageController(BookingPageService bookingPageService) {
        this.bookingPageService = bookingPageService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('appointment:read')")
    @Operation(summary = "Configuracion de reserva online de la empresa; se crea si no existe (requiere appointment:read)")
    public BookingPageView get(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return bookingPageService.getOrCreate(tenantId, companyId);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('appointment:manage')")
    @Operation(summary = "Actualiza la pagina de reserva: campos, logo, textos (requiere appointment:manage)")
    public BookingPageView update(@RequestParam UUID companyId,
                                  @Valid @RequestBody UpdateBookingPageRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return bookingPageService.update(tenantId, companyId, req);
    }

    @PostMapping("/regenerate-token")
    @PreAuthorize("hasAuthority('appointment:manage')")
    @Operation(summary = "Regenera el token del QR, invalidando el enlace anterior (requiere appointment:manage)")
    public BookingPageView regenerate(@RequestParam UUID companyId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return bookingPageService.regenerateToken(tenantId, companyId);
    }
}
