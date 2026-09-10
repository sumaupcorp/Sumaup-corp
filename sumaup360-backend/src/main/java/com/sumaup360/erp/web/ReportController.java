package com.sumaup360.erp.web;

import com.sumaup360.erp.service.ReportService;
import com.sumaup360.erp.service.SaleService;
import com.sumaup360.erp.web.dto.ReportDtos.LowStockResponse;
import com.sumaup360.erp.web.dto.ReportDtos.PaymentMethodStatResponse;
import com.sumaup360.erp.web.dto.ReportDtos.SalesByBranchResponse;
import com.sumaup360.erp.web.dto.ReportDtos.SalesSummaryResponse;
import com.sumaup360.erp.web.dto.ReportDtos.TopProductResponse;
import com.sumaup360.erp.web.dto.SaleDtos.DailySalesResponse;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Reportes ERP del tenant del contexto. Todos requieren report:read.
 * Rango por defecto: ultimos 30 dias si no se envian from/to (ISO-8601).
 */
@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes", description = "Resumenes y KPIs del negocio")
public class ReportController {

    private final ReportService reportService;
    private final SaleService saleService;

    public ReportController(ReportService reportService, SaleService saleService) {
        this.reportService = reportService;
        this.saleService = saleService;
    }

    @GetMapping("/sales-daily")
    @PreAuthorize("hasAuthority('report:read')")
    @Operation(summary = "Ventas por dia de los ultimos N dias, efectivo vs digital (requiere report:read)")
    public List<DailySalesResponse> salesDaily(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "30") int days) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return saleService.dailyFlow(tenantId, branchId, Math.min(Math.max(days, 1), 90)).stream()
                .map(f -> new DailySalesResponse(f.date(), f.total(), f.cashTotal(),
                        f.digitalTotal(), f.count()))
                .toList();
    }

    @GetMapping("/payment-methods")
    @PreAuthorize("hasAuthority('report:read')")
    @Operation(summary = "Ventas por metodo de pago de los ultimos N dias (requiere report:read)")
    public List<PaymentMethodStatResponse> paymentMethods(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "30") int days) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return saleService.paymentBreakdown(tenantId, branchId, Math.min(Math.max(days, 1), 90)).stream()
                .map(p -> new PaymentMethodStatResponse(p.method(), p.count(), p.total()))
                .toList();
    }

    @GetMapping("/sales-summary")
    @PreAuthorize("hasAuthority('report:read')")
    @Operation(summary = "Resumen de ventas (count y total) en un rango (requiere report:read)")
    public SalesSummaryResponse salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) UUID branchId) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return reportService.salesSummary(tenantId, from(from), to(to), branchId);
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAuthority('report:read')")
    @Operation(summary = "Productos mas vendidos en un rango (requiere report:read)")
    public List<TopProductResponse> topProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "5") int limit) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return reportService.topProducts(tenantId, from(from), to(to), branchId, limit);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('report:read')")
    @Operation(summary = "Productos con stock bajo en una sucursal (requiere report:read)")
    public List<LowStockResponse> lowStock(
            @RequestParam UUID branchId,
            @RequestParam(defaultValue = "5") BigDecimal threshold) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return reportService.lowStock(tenantId, branchId, threshold);
    }

    @GetMapping("/sales-by-branch")
    @PreAuthorize("hasAuthority('report:read')")
    @Operation(summary = "Ventas agrupadas por sucursal en un rango (requiere report:read)")
    public List<SalesByBranchResponse> salesByBranch(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return reportService.salesByBranch(tenantId, from(from), to(to));
    }

    private OffsetDateTime from(OffsetDateTime v) {
        return v != null ? v : OffsetDateTime.now().minusDays(30);
    }

    private OffsetDateTime to(OffsetDateTime v) {
        return v != null ? v : OffsetDateTime.now().plusDays(1);
    }
}
