package com.sumaup360.app.feature;

import com.sumaup360.common.error.ApiException;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Chequeo de acceso a features (bloqueo premium). La app llama aqui antes de cada accion
 * premium y, si allowed=false, muestra la pantalla de upgrade del plan correspondiente.
 */
@RestController
@RequestMapping("/api/v1/app/features")
@Tag(name = "Personas - Features", description = "Permisos por plan / tipo de cliente")
public class FeatureController {

    private final FeatureAccessService featureAccessService;

    public FeatureController(FeatureAccessService featureAccessService) {
        this.featureAccessService = featureAccessService;
    }

    @GetMapping("/{feature}/access")
    @Operation(summary = "Indica si el usuario puede usar una feature y, si no, que plan necesita")
    public FeatureAccessService.Access access(@PathVariable String feature) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        Feature f = parse(feature);
        return featureAccessService.check(userId, f);
    }

    private static Feature parse(String value) {
        try {
            return Feature.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Feature desconocida: " + value);
        }
    }
}
