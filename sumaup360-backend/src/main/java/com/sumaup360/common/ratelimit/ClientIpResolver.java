package com.sumaup360.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resuelve la IP real del cliente para el rate-limit. X-Forwarded-For solo se acepta si
 * `public-endpoints.trust-forwarded-headers=true` (backend detras de un proxy propio que
 * sobrescribe el header); si el backend esta expuesto directo, un atacante podria falsificar
 * el header y saltarse el limite por IP.
 */
@Component
public class ClientIpResolver {

    private final PublicEndpointProperties props;

    public ClientIpResolver(PublicEndpointProperties props) {
        this.props = props;
    }

    public String resolve(HttpServletRequest http) {
        if (props.isTrustForwardedHeaders()) {
            String xff = http.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
        }
        return http.getRemoteAddr();
    }
}
