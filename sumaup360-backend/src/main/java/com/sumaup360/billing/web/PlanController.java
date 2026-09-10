package com.sumaup360.billing.web;

import com.sumaup360.billing.dto.BillingDtos.CreatePlanRequest;
import com.sumaup360.billing.dto.BillingDtos.PlanView;
import com.sumaup360.billing.enums.ProductLine;
import com.sumaup360.billing.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Planes. Lectura para usuarios autenticados; creacion solo staff (plan:manage). */
@RestController
@RequestMapping("/api/v1/billing/plans")
@Tag(name = "Planes", description = "Planes comerciales (Personas y Negocios)")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    @Operation(summary = "Lista planes (opcional ?line=PERSON|BUSINESS)")
    public List<PlanView> list(@RequestParam(required = false) ProductLine line) {
        return planService.list(line);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Detalle de un plan por codigo")
    public PlanView get(@PathVariable String code) {
        return planService.getByCode(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('plan:manage')")
    @Operation(summary = "Crea un plan (requiere plan:manage)")
    public PlanView create(@Valid @RequestBody CreatePlanRequest req) {
        return planService.createPlan(req);
    }
}
