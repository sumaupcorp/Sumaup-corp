package com.sumaup360.erp.service;

import com.sumaup360.erp.domain.InventoryStock;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.repository.InventoryStockRepository;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.erp.repository.SaleItemRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.erp.web.dto.ReportDtos.LowStockResponse;
import com.sumaup360.erp.web.dto.ReportDtos.SalesByBranchResponse;
import com.sumaup360.erp.web.dto.ReportDtos.SalesSummaryResponse;
import com.sumaup360.erp.web.dto.ReportDtos.TopProductResponse;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reportes ERP del tenant del contexto: resumen de ventas, top de productos, stock bajo y
 * ventas por sucursal. Solo lectura, agregaciones en la base.
 */
@Service
public class ReportService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final InventoryStockRepository stockRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    public ReportService(SaleRepository saleRepository,
                         SaleItemRepository saleItemRepository,
                         InventoryStockRepository stockRepository,
                         ProductRepository productRepository,
                         BranchRepository branchRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public SalesSummaryResponse salesSummary(UUID tenantId, OffsetDateTime from,
                                             OffsetDateTime to, UUID branchId) {
        List<Object[]> rows = saleRepository.salesSummary(tenantId, from, to, branchId);
        Object[] row = rows.isEmpty() ? new Object[]{0L, BigDecimal.ZERO} : rows.get(0);
        return new SalesSummaryResponse(from, to, branchId, toLong(row[0]), toBigDecimal(row[1]));
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(UUID tenantId, OffsetDateTime from,
                                                OffsetDateTime to, UUID branchId, int limit) {
        List<Object[]> rows = saleItemRepository.topProducts(tenantId, from, to, branchId,
                PageRequest.of(0, Math.max(1, limit)));
        List<UUID> ids = rows.stream().map(r -> (UUID) r[0]).toList();
        Map<UUID, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return rows.stream().map(r -> {
            UUID pid = (UUID) r[0];
            Product p = products.get(pid);
            return new TopProductResponse(pid,
                    p != null ? p.getSku() : null,
                    p != null ? p.getName() : null,
                    toBigDecimal(r[1]), toBigDecimal(r[2]));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock(UUID tenantId, UUID branchId, BigDecimal threshold) {
        List<InventoryStock> stocks = stockRepository
                .findByTenantIdAndBranchIdAndQuantityLessThanEqual(tenantId, branchId, threshold);
        List<UUID> ids = stocks.stream().map(InventoryStock::getProductId).toList();
        Map<UUID, Product> products = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return stocks.stream().map(s -> {
            Product p = products.get(s.getProductId());
            return new LowStockResponse(s.getProductId(),
                    p != null ? p.getSku() : null,
                    p != null ? p.getName() : null,
                    s.getBranchId(), s.getQuantity());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<SalesByBranchResponse> salesByBranch(UUID tenantId, OffsetDateTime from,
                                                     OffsetDateTime to) {
        List<Object[]> rows = saleRepository.salesByBranch(tenantId, from, to);
        List<UUID> ids = rows.stream().map(r -> (UUID) r[0]).toList();
        Map<UUID, Branch> branches = branchRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity()));
        return rows.stream().map(r -> {
            UUID bid = (UUID) r[0];
            Branch b = branches.get(bid);
            return new SalesByBranchResponse(bid,
                    b != null ? b.getName() : null,
                    toLong(r[1]), toBigDecimal(r[2]));
        }).toList();
    }

    private static long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(o.toString());
    }
}
