
package com.sumaup360.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/** Endpoint publico de salud para verificar que el servicio responde. */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Estado del servicio")
public class HealthController {

    @GetMapping
    @Operation(summary = "Estado del backend")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "sumaup360-backend",
                "time", OffsetDateTime.now().toString()
        );
    }
}
