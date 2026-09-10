package com.sumaup360.app.web;

import com.sumaup360.app.dto.ReceiptDtos.CreateReceiptRequest;
import com.sumaup360.app.dto.ReceiptDtos.ReceiptResponse;
import com.sumaup360.app.service.ReceiptService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Recibos subidos por la persona (los procesa el Backoffice). */
@RestController
@RequestMapping("/api/v1/app/receipts")
@Tag(name = "Personas - Recibos", description = "Carga de recibos/comprobantes")
public class ReceiptController {

    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Sube un recibo (queda PENDING para el Backoffice)")
    public ReceiptResponse create(@Valid @RequestBody CreateReceiptRequest req) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ReceiptResponse.from(receiptService.create(userId, req));
    }

    @GetMapping
    @Operation(summary = "Lista los recibos de la persona")
    public List<ReceiptResponse> list() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return receiptService.list(userId).stream().map(ReceiptResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un recibo")
    public ReceiptResponse get(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return ReceiptResponse.from(receiptService.get(id, userId));
    }
}
