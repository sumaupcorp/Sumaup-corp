package com.sumaup360.erp.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.InventoryMovement;
import com.sumaup360.erp.domain.InventoryStock;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.repository.InventoryMovementRepository;
import com.sumaup360.erp.repository.InventoryStockRepository;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inventario: stock por producto y sucursal, con movimientos auditados.
 * Toda operacion valida que producto y sucursal pertenezcan al tenant del contexto.
 */
@Service
public class InventoryService {

    private final InventoryStockRepository stockRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    public InventoryService(InventoryStockRepository stockRepository,
                            InventoryMovementRepository movementRepository,
                            ProductRepository productRepository,
                            BranchRepository branchRepository) {
        this.stockRepository = stockRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional(readOnly = true)
    public BigDecimal getQuantity(UUID tenantId, UUID productId, UUID branchId) {
        requireProduct(tenantId, productId);
        requireBranch(tenantId, branchId);
        return stockRepository.findByTenantIdAndProductIdAndBranchId(tenantId, productId, branchId)
                .map(InventoryStock::getQuantity)
                .orElse(BigDecimal.ZERO);
    }

    /** Todo el stock de una sucursal (los productos sin fila tienen stock cero). */
    @Transactional(readOnly = true)
    public java.util.List<InventoryStock> listByBranch(UUID tenantId, UUID branchId) {
        requireBranch(tenantId, branchId);
        return stockRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    /** Kardex paginado de la sucursal (ventas, ajustes, ingresos), mas reciente primero. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<InventoryMovement> pageMovements(
            UUID tenantId, UUID branchId, int page, int size) {
        requireBranch(tenantId, branchId);
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return movementRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable);
    }

    /** Un dia del flujo de inventario: unidades que entraron y salieron. */
    public record DailyFlow(java.time.LocalDate date, BigDecimal inQty, BigDecimal outQty) {
    }

    /**
     * Flujo de los ultimos 7 dias (hora de Lima): entradas vs salidas por dia,
     * incluyendo los dias sin movimiento (en cero) para graficar la semana completa.
     */
    @Transactional(readOnly = true)
    public java.util.List<DailyFlow> weeklyFlow(UUID tenantId, UUID branchId) {
        requireBranch(tenantId, branchId);
        java.time.ZoneId lima = java.time.ZoneId.of("America/Lima");
        java.time.LocalDate today = java.time.LocalDate.now(lima);
        java.time.LocalDate start = today.minusDays(6);
        java.time.OffsetDateTime from = start.atStartOfDay(lima).toOffsetDateTime();

        java.util.Map<java.time.LocalDate, BigDecimal[]> byDay = new java.util.LinkedHashMap<>();
        for (java.time.LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            byDay.put(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (InventoryMovement m : movementRepository
                .findByTenantIdAndBranchIdAndCreatedAtGreaterThanEqual(tenantId, branchId, from)) {
            java.time.LocalDate day = m.getCreatedAt().atZoneSameInstant(lima).toLocalDate();
            BigDecimal[] acc = byDay.get(day);
            if (acc == null) {
                continue;
            }
            boolean isOut = "OUT".equals(m.getType())
                    || ("ADJUST".equals(m.getType()) && m.getQuantity().signum() < 0);
            if (isOut) {
                acc[1] = acc[1].add(m.getQuantity().abs());
            } else {
                acc[0] = acc[0].add(m.getQuantity().abs());
            }
        }
        return byDay.entrySet().stream()
                .map(e -> new DailyFlow(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }

    /** Ajuste manual de stock (delta con signo). reason explica el motivo. */
    @Transactional
    public InventoryStock adjust(UUID tenantId, UUID productId, UUID branchId,
                                 BigDecimal delta, String reason, UUID userId) {
        requireProduct(tenantId, productId);
        requireBranch(tenantId, branchId);
        if (delta == null || delta.signum() == 0) {
            throw new BadRequestException("La cantidad de ajuste no puede ser cero.");
        }
        InventoryStock stock = getOrCreate(tenantId, productId, branchId);
        BigDecimal newQty = stock.getQuantity().add(delta);
        if (newQty.signum() < 0) {
            throw new BadRequestException("El ajuste dejaria el stock negativo.");
        }
        stock.setQuantity(newQty);
        stockRepository.save(stock);
        record(tenantId, productId, branchId, "ADJUST", delta, reason, null, userId);
        return stock;
    }

    /** Salida de stock por una venta. Falla si no hay stock suficiente. */
    @Transactional
    public void applySaleOut(UUID tenantId, UUID productId, UUID branchId,
                             BigDecimal quantity, UUID saleId, UUID userId) {
        InventoryStock stock = stockRepository
                .findByTenantIdAndProductIdAndBranchId(tenantId, productId, branchId)
                .orElseThrow(() -> new BadRequestException("Sin stock para el producto en la sucursal."));
        if (stock.getQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Stock insuficiente. Disponible: " + stock.getQuantity());
        }
        stock.setQuantity(stock.getQuantity().subtract(quantity));
        stockRepository.save(stock);
        record(tenantId, productId, branchId, "OUT", quantity, "Venta", saleId, userId);
    }

    private InventoryStock getOrCreate(UUID tenantId, UUID productId, UUID branchId) {
        return stockRepository.findByTenantIdAndProductIdAndBranchId(tenantId, productId, branchId)
                .orElseGet(() -> {
                    InventoryStock s = new InventoryStock();
                    s.setTenantId(tenantId);
                    s.setProductId(productId);
                    s.setBranchId(branchId);
                    s.setQuantity(BigDecimal.ZERO);
                    return s;
                });
    }

    private void record(UUID tenantId, UUID productId, UUID branchId, String type,
                        BigDecimal quantity, String reason, UUID refId, UUID userId) {
        InventoryMovement m = new InventoryMovement();
        m.setTenantId(tenantId);
        m.setProductId(productId);
        m.setBranchId(branchId);
        m.setType(type);
        m.setQuantity(quantity);
        m.setReason(reason);
        m.setRefId(refId);
        m.setCreatedBy(userId);
        movementRepository.save(m);
    }

    private Product requireProduct(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }
}
