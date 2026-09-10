package com.sumaup360.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI. Define el esquema Bearer para enviar el idToken de Firebase
 * (o el header de modo dev) desde el boton "Authorize".
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI sumaup360OpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SUMAUP360 API")
                        .description("Backend monolito modular. Autoridad de identidad, RBAC, "
                                + "tenant y membresia. Firebase prueba la identidad; el backend autoriza.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("idToken de Firebase. En modo dev local tambien se "
                                        + "acepta el header X-Debug-Uid.")));
    }
}
