package com.sumaup360.catalog.web;

import com.sumaup360.catalog.dto.CatalogDtos.CodeName;
import com.sumaup360.catalog.dto.CatalogDtos.CurrencyView;
import com.sumaup360.catalog.dto.CatalogDtos.ModuleView;
import com.sumaup360.catalog.dto.CatalogDtos.VerticalView;
import com.sumaup360.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogos compartidos (globales). Requieren estar autenticado, pero no un permiso especial:
 * son datos de referencia para onboarding y configuracion (rubros, modulos, monedas...).
 */
@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalogos", description = "Datos de referencia compartidos del ecosistema")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/currencies")
    @Operation(summary = "Monedas")
    public List<CurrencyView> currencies() {
        return catalogService.currencies();
    }

    @GetMapping("/document-types")
    @Operation(summary = "Tipos de comprobante/documento")
    public List<CodeName> documentTypes() {
        return catalogService.documentTypes();
    }

    @GetMapping("/business-types")
    @Operation(summary = "Rubros de negocio")
    public List<CodeName> businessTypes() {
        return catalogService.businessTypes();
    }

    @GetMapping("/verticals")
    @Operation(summary = "Sub-rubros/verticales (opcional ?businessType=codigo)")
    public List<VerticalView> verticals(@RequestParam(required = false) String businessType) {
        return catalogService.verticals(businessType);
    }

    @GetMapping("/modules")
    @Operation(summary = "Modulos del sistema")
    public List<ModuleView> modules() {
        return catalogService.modules();
    }

    @GetMapping("/person-segments")
    @Operation(summary = "Segmentos de persona (Linea Personas)")
    public List<CodeName> personSegments() {
        return catalogService.personSegments();
    }
}
