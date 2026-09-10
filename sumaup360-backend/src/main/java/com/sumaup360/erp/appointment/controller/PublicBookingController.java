package com.sumaup360.erp.appointment.controller;

import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.ratelimit.ClientIpResolver;
import com.sumaup360.common.ratelimit.PublicEndpointProperties;
import com.sumaup360.common.ratelimit.SimpleRateLimiter;
import com.sumaup360.erp.appointment.dto.BookingDtos.PublicBookingCreated;
import com.sumaup360.erp.appointment.dto.BookingDtos.PublicBookingInfo;
import com.sumaup360.erp.appointment.dto.BookingDtos.PublicBookingRequest;
import com.sumaup360.erp.appointment.service.PublicBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reserva de citas desde la web publica (QR de la empresa). SIN autenticacion
 * (/api/v1/public/** esta permitido en SecurityConfig) y protegido con rate-limit
 * por IP y por token + tope diario por token.
 */
@RestController
@RequestMapping("/api/v1/public/booking")
@Tag(name = "Publico - Reserva de citas", description = "Formulario publico de reserva (QR)")
public class PublicBookingController {

    private final PublicBookingService publicBookingService;
    private final SimpleRateLimiter rateLimiter;
    private final ClientIpResolver clientIp;
    private final PublicEndpointProperties limits;

    public PublicBookingController(PublicBookingService publicBookingService,
                                   SimpleRateLimiter rateLimiter,
                                   ClientIpResolver clientIp,
                                   PublicEndpointProperties limits) {
        this.publicBookingService = publicBookingService;
        this.rateLimiter = rateLimiter;
        this.clientIp = clientIp;
        this.limits = limits;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Datos de la pagina de reserva: titulo, logo, campos, sucursales")
    public PublicBookingInfo info(@PathVariable String token, HttpServletRequest http) {
        if (!rateLimiter.allow("booking-info:" + clientIp.resolve(http), 30)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas consultas. Intenta en un momento.");
        }
        return publicBookingService.info(token);
    }

    @PostMapping("/{token}/request")
    @Operation(summary = "Crea una solicitud de cita (queda pendiente de confirmacion del negocio)")
    public PublicBookingCreated create(@PathVariable String token,
                                       @Valid @RequestBody PublicBookingRequest req,
                                       HttpServletRequest http) {
        if (!rateLimiter.allow("booking-req-token:" + token, 15)
                || !rateLimiter.allow("booking-req-ip:" + clientIp.resolve(http), 10)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Intenta en un momento.");
        }
        if (!rateLimiter.allowDaily("booking-req-token:" + token, limits.getBookingDailyLimit())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Este negocio alcanzo su limite de reservas online por hoy. Intenta manana o contactalo directamente.");
        }
        return publicBookingService.createRequest(token, req);
    }
}
