package com.sumaup360.app.web;

import com.sumaup360.app.dto.IncomeDtos.CreateIncomeRequest;
import com.sumaup360.app.dto.IncomeDtos.IncomeResponse;
import com.sumaup360.app.service.IncomeService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Ingresos de la persona. */
@RestController
@RequestMapping("/api/v1/app/income")
@Tag(name = "Personas - Ingresos", description = "Registro de ingresos")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un ingreso")
    public IncomeResponse create(@Valid @RequestBody CreateIncomeRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return IncomeResponse.from(incomeService.create(userId, req));
    }

    @GetMapping
    @Operation(summary = "Lista los ingresos de la persona")
    public List<IncomeResponse> list() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return incomeService.list(userId).stream().map(IncomeResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un ingreso")
    public IncomeResponse get(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return IncomeResponse.from(incomeService.get(id, userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un ingreso")
    public void delete(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        incomeService.delete(id, userId);
    }
}
