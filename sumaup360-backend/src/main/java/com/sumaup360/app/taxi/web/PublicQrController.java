package com.sumaup360.app.taxi.web;

import com.sumaup360.app.taxi.domain.ReceiptRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.CreatePublicRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.CreatedView;
import com.sumaup360.app.taxi.dto.TaxiDtos.CustomerLookupView;
import com.sumaup360.app.taxi.dto.TaxiDtos.QrPublicView;
import com.sumaup360.app.taxi.service.PublicQrService;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.ratelimit.ClientIpResolver;
import com.sumaup360.common.ratelimit.PublicEndpointProperties;
import com.sumaup360.common.ratelimit.SimpleRateLimiter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints PUBLICOS (sin auth) para el cliente final que escanea el QR del taxista.
 * Protegidos con rate-limit por IP y por token + tope diario (anti abuso/enumeracion).
 * No exponen datos de contacto de terceros.
 */
@RestController
@RequestMapping("/api/v1/public/qr")
@Tag(name = "Publico - QR Taxista", description = "Solicitud de comprobante del cliente final")
public class PublicQrController {

    private final PublicQrService publicQrService;
    private final SimpleRateLimiter rateLimiter;
    private final ClientIpResolver clientIp;
    private final PublicEndpointProperties limits;

    public PublicQrController(PublicQrService publicQrService, SimpleRateLimiter rateLimiter,
                              ClientIpResolver clientIp, PublicEndpointProperties limits) {
        this.publicQrService = publicQrService;
        this.rateLimiter = rateLimiter;
        this.clientIp = clientIp;
        this.limits = limits;
    }

    @GetMapping("/taxi/{token}")
    @Operation(summary = "Valida un QR de taxista y devuelve el nombre del taxista")
    public QrPublicView info(@PathVariable String token, HttpServletRequest http) {
        if (!rateLimiter.allow("qr-info:" + clientIp.resolve(http), 30)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas consultas. Intenta en un momento.");
        }
        return publicQrService.info(token);
    }

    @GetMapping("/taxi/{token}/customers/lookup")
    @Operation(summary = "Precarga el nombre del cliente por documento (requiere QR valido)")
    public CustomerLookupView lookup(@PathVariable String token, @RequestParam("doc") String doc,
                                     HttpServletRequest http) {
        if (!rateLimiter.allow("qr-lookup:" + clientIp.resolve(http), 30)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas consultas. Intenta en un momento.");
        }
        return CustomerLookupView.of(publicQrService.lookup(token, doc));
    }

    @PostMapping("/taxi/{token}/request")
    @Operation(summary = "Crea una solicitud de comprobante")
    public CreatedView createRequest(@PathVariable String token, @Valid @RequestBody CreatePublicRequest req,
                                     HttpServletRequest http) {
        if (!rateLimiter.allow("qr-req-token:" + token, 15)
                || !rateLimiter.allow("qr-req-ip:" + clientIp.resolve(http), 30)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Intenta en un momento.");
        }
        if (!rateLimiter.allowDaily("qr-req-token:" + token, limits.getQrDailyLimit())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Este enlace alcanzo su limite de solicitudes por hoy. Intenta manana.");
        }
        ReceiptRequest r = publicQrService.createRequest(token, req);
        return new CreatedView(r.getId(), r.getEstado().name());
    }
}
