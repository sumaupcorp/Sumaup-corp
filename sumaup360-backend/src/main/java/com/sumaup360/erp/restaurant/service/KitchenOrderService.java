package com.sumaup360.erp.restaurant.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.CashSession;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.domain.SaleItem;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.erp.repository.SaleItemRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.erp.service.CashSessionService;
import com.sumaup360.erp.restaurant.domain.KitchenOrder;
import com.sumaup360.erp.restaurant.domain.KitchenOrderItem;
import com.sumaup360.erp.restaurant.domain.RestaurantTable;
import com.sumaup360.erp.restaurant.enums.OrderItemStatus;
import com.sumaup360.erp.restaurant.enums.OrderStatus;
import com.sumaup360.erp.restaurant.enums.OrderType;
import com.sumaup360.erp.restaurant.enums.TableStatus;
import com.sumaup360.erp.restaurant.repository.KitchenOrderItemRepository;
import com.sumaup360.erp.restaurant.repository.KitchenOrderRepository;
import com.sumaup360.erp.restaurant.repository.RestaurantTableRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Comandas (kitchen_order). Maneja el ciclo mesa -> comanda -> cocina -> cobro.
 * El cobro genera una venta (erp.sale) SIN descontar stock (los platos no llevan inventario
 * 1:1; el control de insumos por receta es un TODO futuro).
 */
@Service
public class KitchenOrderService {

    private final KitchenOrderRepository orderRepository;
    private final KitchenOrderItemRepository itemRepository;
    private final RestaurantTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final CashSessionService cashSessionService;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;

    public KitchenOrderService(KitchenOrderRepository orderRepository,
                               KitchenOrderItemRepository itemRepository,
                               RestaurantTableRepository tableRepository,
                               ProductRepository productRepository,
                               BranchRepository branchRepository,
                               CashSessionService cashSessionService,
                               SaleRepository saleRepository,
                               SaleItemRepository saleItemRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.cashSessionService = cashSessionService;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
    }

    @Transactional
    public KitchenOrder createOrder(UUID tenantId, UUID branchId, OrderType type,
                                    UUID tableId, String notes, UUID userId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));

        if (type == OrderType.DINE_IN) {
            if (tableId == null) {
                throw new BadRequestException("Una comanda en mesa requiere tableId.");
            }
            RestaurantTable table = requireTable(tenantId, tableId, branchId);
            table.setStatus(TableStatus.OCCUPIED);
            tableRepository.save(table);
        }

        KitchenOrder order = new KitchenOrder();
        order.setTenantId(tenantId);
        order.setBranchId(branchId);
        order.setTableId(type == OrderType.DINE_IN ? tableId : null);
        order.setType(type);
        order.setStatus(OrderStatus.OPEN);
        order.setNotes(notes);
        order.setTotal(BigDecimal.ZERO);
        order.setCreatedBy(userId);
        order.setOrderNumber(String.format("CMD-%06d", orderRepository.countByBranchId(branchId) + 1));
        return orderRepository.save(order);
    }

    @Transactional
    public KitchenOrderItem addItem(UUID tenantId, UUID orderId, UUID productId,
                                    BigDecimal quantity, String notes, String station) {
        KitchenOrder order = requireOrder(tenantId, orderId);
        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.IN_KITCHEN) {
            throw new BadRequestException("No se pueden agregar items a una comanda en estado " + order.getStatus());
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new BadRequestException("La cantidad debe ser mayor a cero.");
        }
        Product product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));

        KitchenOrderItem item = new KitchenOrderItem();
        item.setTenantId(tenantId);
        item.setKitchenOrderId(order.getId());
        item.setProductId(product.getId());
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setLineTotal(product.getPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP));
        item.setNotes(notes);
        item.setStation(station);
        item.setStatus(OrderItemStatus.PENDING);
        KitchenOrderItem saved = itemRepository.save(item);

        recomputeTotal(order);
        return saved;
    }

    @Transactional
    public KitchenOrder sendToKitchen(UUID tenantId, UUID orderId) {
        KitchenOrder order = requireOrder(tenantId, orderId);
        if (order.getStatus() != OrderStatus.OPEN) {
            throw new BadRequestException("Solo una comanda OPEN puede enviarse a cocina.");
        }
        if (itemRepository.findByKitchenOrderId(order.getId()).isEmpty()) {
            throw new BadRequestException("La comanda no tiene items.");
        }
        order.setStatus(OrderStatus.IN_KITCHEN);
        return orderRepository.save(order);
    }

    @Transactional
    public KitchenOrder cancelOrder(UUID tenantId, UUID orderId) {
        KitchenOrder order = requireOrder(tenantId, orderId);
        if (order.getStatus() == OrderStatus.BILLED) {
            throw new BadRequestException("No se puede cancelar una comanda ya cobrada.");
        }
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        freeTable(tenantId, order);
        return order;
    }

    /** Cobra la comanda: genera venta y libera la mesa. Requiere caja abierta. */
    @Transactional
    public KitchenOrder billOrder(UUID tenantId, UUID orderId, UUID customerId, UUID userId) {
        KitchenOrder order = requireOrder(tenantId, orderId);
        if (order.getStatus() == OrderStatus.BILLED) {
            throw new BadRequestException("La comanda ya fue cobrada.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("La comanda esta cancelada.");
        }
        List<KitchenOrderItem> items = itemRepository.findByKitchenOrderId(order.getId());
        if (items.isEmpty()) {
            throw new BadRequestException("La comanda no tiene items.");
        }
        CashSession session = cashSessionService.requireOpen(tenantId, order.getBranchId());

        Sale sale = new Sale();
        sale.setTenantId(tenantId);
        sale.setBranchId(order.getBranchId());
        sale.setCashSessionId(session.getId());
        sale.setCustomerId(customerId);
        sale.setStatus("COMPLETED");
        sale.setCreatedBy(userId);
        sale.setTotal(order.getTotal());
        sale = saleRepository.save(sale);

        for (KitchenOrderItem it : items) {
            SaleItem si = new SaleItem();
            si.setTenantId(tenantId);
            si.setSaleId(sale.getId());
            si.setProductId(it.getProductId());
            si.setQuantity(it.getQuantity());
            si.setUnitPrice(it.getUnitPrice());
            si.setLineTotal(it.getLineTotal());
            saleItemRepository.save(si);
        }
        // Nota: no se descuenta stock (platos preparados). TODO: descuento de insumos por receta.

        order.setSaleId(sale.getId());
        order.setStatus(OrderStatus.BILLED);
        orderRepository.save(order);
        freeTable(tenantId, order);
        return order;
    }

    @Transactional(readOnly = true)
    public List<KitchenOrder> listOrders(UUID tenantId, UUID branchId, OrderStatus status) {
        return status == null
                ? orderRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : orderRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
    }

    @Transactional(readOnly = true)
    public KitchenOrder getOrder(UUID tenantId, UUID orderId) {
        return requireOrder(tenantId, orderId);
    }

    @Transactional(readOnly = true)
    public List<KitchenOrderItem> listItems(UUID orderId) {
        return itemRepository.findByKitchenOrderId(orderId);
    }

    // ---- helpers ----

    private void recomputeTotal(KitchenOrder order) {
        BigDecimal total = itemRepository.findByKitchenOrderId(order.getId()).stream()
                .map(KitchenOrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        orderRepository.save(order);
    }

    private void freeTable(UUID tenantId, KitchenOrder order) {
        if (order.getTableId() != null) {
            tableRepository.findByIdAndTenantId(order.getTableId(), tenantId).ifPresent(t -> {
                t.setStatus(TableStatus.FREE);
                tableRepository.save(t);
            });
        }
    }

    private KitchenOrder requireOrder(UUID tenantId, UUID orderId) {
        return orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda no encontrada."));
    }

    private RestaurantTable requireTable(UUID tenantId, UUID tableId, UUID branchId) {
        RestaurantTable table = tableRepository.findByIdAndTenantId(tableId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada."));
        if (!table.getBranchId().equals(branchId)) {
            throw new BadRequestException("La mesa no pertenece a la sucursal indicada.");
        }
        return table;
    }
}
