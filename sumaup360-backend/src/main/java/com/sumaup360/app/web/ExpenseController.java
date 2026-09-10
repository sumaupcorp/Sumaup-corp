package com.sumaup360.app.web;

import com.sumaup360.app.dto.ExpenseDtos.CreateExpenseRequest;
import com.sumaup360.app.dto.ExpenseDtos.ExpenseResponse;
import com.sumaup360.app.service.ExpenseService;
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

/** Gastos de la persona. */
@RestController
@RequestMapping("/api/v1/app/expenses")
@Tag(name = "Personas - Gastos", description = "Registro de gastos")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un gasto")
    public ExpenseResponse create(@Valid @RequestBody CreateExpenseRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ExpenseResponse.from(expenseService.create(userId, req));
    }

    @GetMapping
    @Operation(summary = "Lista los gastos de la persona")
    public List<ExpenseResponse> list() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return expenseService.list(userId).stream().map(ExpenseResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un gasto")
    public ExpenseResponse get(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ExpenseResponse.from(expenseService.get(id, userId));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un gasto")
    public void delete(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        expenseService.delete(id, userId);
    }
}
