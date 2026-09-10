package com.sumaup360.erp.web;

import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.service.SaleService;
import com.sumaup360.erp.service.SaleService.SaleLineCommand;
import com.sumaup360.erp.service.SaleTicketService;
import com.sumaup360.erp.web.dto.SaleDtos.CreateSaleRequest;
import com.sumaup360.erp.web.dto.SaleDtos.SaleResponse;
import com.sumaup360.security.SecurityUtils;
import com.sumaup360.tenant.service.BranchAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Ventas (POS) del tenant del contexto. */
@RestController
@RequestMapping("/api/v1/sales")
@Tag(name = "Ventas", description = "Registro de ventas (descuenta stock, exige caja abierta)")
public class SaleController {

    private final SaleService saleService;
    private final SaleTicketService saleTicketService;
    private final BranchAccessService branchAccessService;

    public SaleController(SaleService saleService,
                          SaleTicketService saleTicketService,
                          BranchAccessService branchAccessService) {
        this.saleService = saleService;
        this.saleTicketService = saleTicketService;
        this.branchAccessService = branchAccessService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('sale:create')")
    @Operation(summary = "Registra una venta (requiere sale:create; respeta la sede asignada)")
    public SaleResponse create(@Valid @RequestBody CreateSaleRequest req) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        UUID userId = SecurityUtils.currentPrincipal().userId();
        branchAccessService.assertCanOperate(tenantId, userId, req.branchId());
        List<SaleLineCommand> lines = req.items().stream()
                .map(i -> new SaleLineCommand(i.productId(), i.quantity()))
                .toList();
        Sale sale = saleService.create(tenantId, req.branchId(), req.customerId(), lines,
                req.paymentMethod(), userId);
        return SaleResponse.from(sale, saleService.listItems(sale.getId()));
    }

    @GetMapping("/flow/weekly")
    @PreAuthorize("hasAuthority('sale:read')")
    @Operation(summary = "Ventas de los ultimos 7 dias por dia: total, efectivo y digital (requiere sale:read)")
    public List<com.sumaup360.erp.web.dto.SaleDtos.DailySalesResponse> weeklyFlow(
            @org.springframework.web.bind.annotation.RequestParam UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return saleService.weeklyFlow(tenantId, branchId).stream()
                .map(f -> new com.sumaup360.erp.web.dto.SaleDtos.DailySalesResponse(
                        f.date(), f.total(), f.cashTotal(), f.digitalTotal(), f.count()))
                .toList();
    }

    @GetMapping(value = "/{id}/ticket", produces = MediaType.TEXT_HTML_VALUE)
    @PreAuthorize("hasAuthority('sale:read')")
    @Operation(summary = "Ticket de la venta en HTML, con la plantilla configurada (requiere sale:read)")
    public String ticketHtml(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return saleTicketService.renderHtml(tenantId, id).html();
    }

    @GetMapping(value = "/{id}/ticket.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('sale:read')")
    @Operation(summary = "Ticket de la venta en PDF, con la plantilla configurada (requiere sale:read)")
    public ResponseEntity<byte[]> ticketPdf(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        byte[] pdf = saleTicketService.renderPdf(tenantId, id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.inline().filename("ticket-" + id + ".pdf").build());
        return ResponseEntity.ok().headers(headers)
                .contentType(MediaType.APPLICATION_PDF).body(pdf);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sale:read')")
    @Operation(summary = "Lista ventas paginadas, mas recientes primero; filtros por sucursal o caja (requiere sale:read)")
    public com.sumaup360.common.web.PageResponse<SaleResponse> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false) UUID branchId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) UUID cashSessionId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer size) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        int p = com.sumaup360.common.web.PageResponse.sanitizePage(page);
        int s = com.sumaup360.common.web.PageResponse.sanitizeSize(size, 20, 100);
        return com.sumaup360.common.web.PageResponse.from(
                saleService.search(tenantId, branchId, cashSessionId, p, s),
                sale -> SaleResponse.from(sale, saleService.listItems(sale.getId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sale:read')")
    @Operation(summary = "Obtiene una venta con sus items (requiere sale:read)")
    public SaleResponse get(@PathVariable UUID id) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        Sale sale = saleService.get(id, tenantId);
        return SaleResponse.from(sale, saleService.listItems(sale.getId()));
    }
}
