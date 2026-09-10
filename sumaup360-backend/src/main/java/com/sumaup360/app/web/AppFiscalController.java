package com.sumaup360.app.web;

import com.sumaup360.app.dto.FiscalSolDtos.DniRefreshRequest;
import com.sumaup360.app.dto.FiscalSolDtos.RucFiscalView;
import com.sumaup360.app.dto.FiscalSolDtos.RucRefreshRequest;
import com.sumaup360.app.dto.FiscalSolDtos.SolCredentialsView;
import com.sumaup360.app.dto.FiscalSolDtos.SolValidationView;
import com.sumaup360.app.dto.FiscalSolDtos.UpsertSolRequest;
import com.sumaup360.app.dto.FiscalSolDtos.ValidateSolRequest;
import com.sumaup360.app.service.AppFiscalService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Credenciales SUNAT (Clave SOL) de la persona. Linea Personas: scopeado por usuario,
 * solo autenticado. La clave se guarda cifrada y no se devuelve en claro.
 */
@RestController
@RequestMapping("/api/v1/app/fiscal")
@Tag(name = "Personas - SUNAT", description = "Conexion de la cuenta SUNAT / Clave SOL de la persona")
public class AppFiscalController {

    private final AppFiscalService fiscalService;

    public AppFiscalController(AppFiscalService fiscalService) {
        this.fiscalService = fiscalService;
    }

    @GetMapping("/sol")
    @Operation(summary = "Estado de la conexion SUNAT (enmascarado, sin clave)")
    public SolCredentialsView get() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return fiscalService.get(userId);
    }

    @PutMapping("/sol")
    @Operation(summary = "Guarda o actualiza la cuenta SUNAT / Clave SOL (cifrada en reposo)")
    public SolCredentialsView upsert(@Valid @RequestBody UpsertSolRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return fiscalService.upsert(userId, req);
    }

    @PostMapping("/sol/validate")
    @Operation(summary = "Valida la Clave SOL con un login real en SUNAT (no guarda nada)")
    public SolValidationView validate(@Valid @RequestBody ValidateSolRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return fiscalService.validateSol(userId, req);
    }

    @GetMapping("/ruc")
    @Operation(summary = "Datos de la Ficha RUC guardados en el perfil")
    public RucFiscalView getRuc() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return fiscalService.getRucFiscal(userId);
    }

    @PostMapping("/ruc-refresh")
    @Operation(summary = "Consulta SUNAT y actualiza los datos de la Ficha RUC en el perfil")
    public RucFiscalView refreshRuc(@Valid @RequestBody(required = false) RucRefreshRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return fiscalService.refreshRuc(userId, req == null ? null : req.ruc());
    }

    @PostMapping("/dni-refresh")
    @Operation(summary = "Consulta SUNAT por DNI (resuelve el RUC) y guarda los datos en el perfil")
    public RucFiscalView refreshByDni(@Valid @RequestBody DniRefreshRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return fiscalService.refreshByDni(userId, req.dni(), req.docType());
    }
}
