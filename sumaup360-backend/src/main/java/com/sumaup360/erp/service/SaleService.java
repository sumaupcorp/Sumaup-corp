package com.sumaup360.erp.service;

import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.documenttemplate.enums.DocumentStatus;
import com.sumaup360.erp.documenttemplate.enums.DocumentType;
import com.sumaup360.erp.documenttemplate.model.IssuedDocument;
import com.sumaup360.erp.documenttemplate.repository.IssuedDocumentRepository;
import com.sumaup360.erp.documenttemplate.service.DocumentSeriesService;
import com.sumaup360.erp.domain.CashSession;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.domain.SaleItem;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.erp.repository.SaleItemRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Ventas (POS). Crear una venta es atomico: valida productos, exige caja abierta,
 * calcula totales y descuenta el stock. Si algo falla, no se registra nada.
 */
@Service
public class SaleService {

    /** Linea de venta de entrada: producto y cantidad. El precio sale del producto. */
    public record SaleLineCommand(UUID productId, BigDecimal quantity) {
    }

    /**
     * Linea generica: con producto (precio propio opcional, descuenta stock) o de
     * precio libre (descripcion + precio, sin producto). Usada por modulos que
     * liquidan cuentas como venta POS (p.ej. check-out de hospedaje).
     */
    public record SaleLine(UUID productId, String description, BigDecimal quantity,
                           BigDecimal unitPrice) {
    }

    /** Metodos de pago aceptados en el POS. Solo CASH suma al efectivo del arqueo. */
    public static final Set<String> PAYMENT_METHODS =
            Set.of("CASH", "CARD", "YAPE", "PLIN", "TRANSFER");

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;
    private final InventoryService inventoryService;
    private final CashSessionService cashSessionService;
    private final DocumentSeriesService documentSeriesService;
    private final IssuedDocumentRepository issuedDocumentRepository;

    public SaleService(SaleRepository saleRepository,
                       SaleItemRepository saleItemRepository,
                       ProductRepository productRepository,
                       CustomerRepository customerRepository,
                       BranchRepository branchRepository,
                       InventoryService inventoryService,
                       CashSessionService cashSessionService,
                       DocumentSeriesService documentSeriesService,
                       IssuedDocumentRepository issuedDocumentRepository) {
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
        this.inventoryService = inventoryService;
        this.cashSessionService = cashSessionService;
        this.documentSeriesService = documentSeriesService;
        this.issuedDocumentRepository = issuedDocumentRepository;
    }

    @Transactional
    public Sale create(UUID tenantId, UUID branchId, UUID customerId,
                       List<SaleLineCommand> lines, String paymentMethod, UUID userId) {
        List<SaleLine> mapped = lines == null ? List.of() : lines.stream()
                .map(l -> new SaleLine(l.productId(), null, l.quantity(), null))
                .toList();
        return createWithLines(tenantId, branchId, customerId, mapped, paymentMethod, userId);
    }

    /**
     * Venta con lineas genericas: de producto (precio del producto salvo override,
     * descuenta stock) o de precio libre (descripcion + precio, sin stock). Misma
     * maquinaria para todas: caja abierta, totales, ticket con serie/correlativo.
     */
    @Transactional
    public Sale createWithLines(UUID tenantId, UUID branchId, UUID customerId,
                                List<SaleLine> lines, String paymentMethod, UUID userId) {
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
        if (lines == null || lines.isEmpty()) {
            throw new BadRequestException("La venta debe tener al menos un item.");
        }
        String method = paymentMethod == null || paymentMethod.isBlank()
                ? "CASH" : paymentMethod.toUpperCase();
        if (!PAYMENT_METHODS.contains(method)) {
            throw new BadRequestException("Metodo de pago invalido.");
        }
        CashSession session = cashSessionService.requireOpen(tenantId, branchId);
        if (customerId != null) {
            customerRepository.findByIdAndTenantId(customerId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
        }

        // Cabecera primero (necesitamos el id para items y movimientos).
        Sale sale = new Sale();
        sale.setTenantId(tenantId);
        sale.setBranchId(branchId);
        sale.setCashSessionId(session.getId());
        sale.setCustomerId(customerId);
        sale.setStatus("COMPLETED");
        sale.setPaymentMethod(method);
        sale.setCreatedBy(userId);
        sale.setTotal(BigDecimal.ZERO);
        sale = saleRepository.save(sale);

        BigDecimal total = BigDecimal.ZERO;
        for (SaleLine line : lines) {
            if (line.quantity() == null || line.quantity().signum() <= 0) {
                throw new BadRequestException("La cantidad de cada item debe ser mayor a cero.");
            }

            SaleItem item = new SaleItem();
            item.setTenantId(tenantId);
            item.setSaleId(sale.getId());
            item.setQuantity(line.quantity());

            BigDecimal unitPrice;
            if (line.productId() != null) {
                Product product = productRepository.findByIdAndTenantId(line.productId(), tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Producto no encontrado: " + line.productId()));
                unitPrice = line.unitPrice() != null ? line.unitPrice() : product.getPrice();
                item.setProductId(product.getId());
            } else {
                if (line.description() == null || line.description().isBlank()) {
                    throw new BadRequestException("Las lineas sin producto necesitan descripcion.");
                }
                if (line.unitPrice() == null) {
                    throw new BadRequestException("Las lineas sin producto necesitan precio.");
                }
                unitPrice = line.unitPrice();
                item.setDescription(line.description().trim());
            }

            BigDecimal lineTotal = unitPrice.multiply(line.quantity()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(lineTotal);
            item.setUnitPrice(unitPrice);
            item.setLineTotal(lineTotal);
            saleItemRepository.save(item);

            // Descuenta stock (falla la venta completa si no alcanza).
            if (line.productId() != null) {
                inventoryService.applySaleOut(tenantId, line.productId(), branchId,
                        line.quantity(), sale.getId(), userId);
            }
        }

        sale.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        sale = saleRepository.save(sale);
        issueTicket(sale, branch, customerId);
        return sale;
    }

    /**
     * Emite el ticket interno de la venta (POS_TICKET). Si la empresa tiene una serie
     * activa para tickets, consume su correlativo (T001-00000123); si no, el documento
     * se emite sin numeracion oficial. La emision es parte de la misma transaccion.
     */
    private void issueTicket(Sale sale, Branch branch, UUID customerId) {
        IssuedDocument doc = new IssuedDocument();
        doc.setTenantId(sale.getTenantId());
        doc.setCompanyId(branch.getCompanyId());
        doc.setBranchId(branch.getId());
        doc.setDocumentType(DocumentType.POS_TICKET);
        doc.setDocumentStatus(DocumentStatus.ISSUED);
        doc.setCustomerId(customerId);
        doc.setSaleId(sale.getId());
        // Precio final con IGV incluido (practica retail peruana): base = total / 1.18.
        BigDecimal total = sale.getTotal();
        BigDecimal subtotal = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        doc.setSubtotal(subtotal);
        doc.setIgv(total.subtract(subtotal));
        doc.setTotal(total);
        doc.setCurrency("PEN");
        doc.setIssuedAt(OffsetDateTime.now());
        documentSeriesService.consumeNext(sale.getTenantId(), branch.getCompanyId(),
                        DocumentType.POS_TICKET)
                .ifPresent(n -> {
                    doc.setSeries(n.series());
                    doc.setNumber(n.number());
                    doc.setFullNumber(n.fullNumber());
                });
        issuedDocumentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<Sale> list(UUID tenantId) {
        return saleRepository.findByTenantId(tenantId);
    }

    /** Un dia del flujo de ventas: total, efectivo, digital (tarjeta/Yape/Plin/transferencia). */
    public record DailySales(java.time.LocalDate date, BigDecimal total,
                             BigDecimal cashTotal, BigDecimal digitalTotal, long count) {
    }

    /** Ventas de los ultimos 7 dias de una sucursal (pestana Caja del POS). */
    @Transactional(readOnly = true)
    public List<DailySales> weeklyFlow(UUID tenantId, UUID branchId) {
        return dailyFlow(tenantId, branchId, 7);
    }

    /**
     * Ventas por dia de los ultimos N dias (hora de Lima), separando efectivo de pagos
     * digitales. branchId null = consolidado de todas las sucursales. Incluye los dias
     * sin ventas (en cero) para graficar el rango completo.
     */
    @Transactional(readOnly = true)
    public List<DailySales> dailyFlow(UUID tenantId, UUID branchId, int days) {
        java.time.ZoneId lima = java.time.ZoneId.of("America/Lima");
        java.time.LocalDate today = java.time.LocalDate.now(lima);
        java.time.LocalDate start = today.minusDays(Math.max(1, days) - 1L);
        OffsetDateTime from = start.atStartOfDay(lima).toOffsetDateTime();

        java.util.Map<java.time.LocalDate, BigDecimal[]> byDay = new java.util.LinkedHashMap<>();
        java.util.Map<java.time.LocalDate, Long> countByDay = new java.util.LinkedHashMap<>();
        for (java.time.LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            byDay.put(d, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            countByDay.put(d, 0L);
        }
        for (Sale v : salesSince(tenantId, branchId, from)) {
            java.time.LocalDate day = v.getCreatedAt().atZoneSameInstant(lima).toLocalDate();
            BigDecimal[] acc = byDay.get(day);
            if (acc == null) {
                continue;
            }
            if ("CASH".equals(v.getPaymentMethod())) {
                acc[0] = acc[0].add(v.getTotal());
            } else {
                acc[1] = acc[1].add(v.getTotal());
            }
            countByDay.merge(day, 1L, Long::sum);
        }
        return byDay.entrySet().stream()
                .map(e -> new DailySales(e.getKey(),
                        e.getValue()[0].add(e.getValue()[1]),
                        e.getValue()[0], e.getValue()[1],
                        countByDay.getOrDefault(e.getKey(), 0L)))
                .toList();
    }

    /** Total y cantidad de ventas por metodo de pago en los ultimos N dias. */
    public record PaymentStat(String method, long count, BigDecimal total) {
    }

    @Transactional(readOnly = true)
    public List<PaymentStat> paymentBreakdown(UUID tenantId, UUID branchId, int days) {
        java.time.ZoneId lima = java.time.ZoneId.of("America/Lima");
        OffsetDateTime from = java.time.LocalDate.now(lima)
                .minusDays(Math.max(1, days) - 1L).atStartOfDay(lima).toOffsetDateTime();
        java.util.Map<String, BigDecimal> totals = new java.util.LinkedHashMap<>();
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (Sale v : salesSince(tenantId, branchId, from)) {
            String method = v.getPaymentMethod() != null ? v.getPaymentMethod() : "CASH";
            totals.merge(method, v.getTotal(), BigDecimal::add);
            counts.merge(method, 1L, Long::sum);
        }
        return totals.entrySet().stream()
                .map(e -> new PaymentStat(e.getKey(), counts.get(e.getKey()), e.getValue()))
                .sorted((a, b) -> b.total().compareTo(a.total()))
                .toList();
    }

    /** Ventas COMPLETED desde una fecha, de una sucursal (validada) o de todo el tenant. */
    private List<Sale> salesSince(UUID tenantId, UUID branchId, OffsetDateTime from) {
        List<Sale> sales;
        if (branchId != null) {
            branchRepository.findByIdAndTenantId(branchId, tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
            sales = saleRepository.findByTenantIdAndBranchIdAndCreatedAtGreaterThanEqual(
                    tenantId, branchId, from);
        } else {
            sales = saleRepository.findByTenantIdAndCreatedAtGreaterThanEqual(tenantId, from);
        }
        return sales.stream().filter(s -> "COMPLETED".equals(s.getStatus())).toList();
    }

    /** Listado paginado (mas reciente primero) con filtros opcionales por sucursal o caja. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Sale> search(UUID tenantId, UUID branchId,
                                                             UUID cashSessionId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        if (cashSessionId != null) {
            return saleRepository.findByTenantIdAndCashSessionId(tenantId, cashSessionId, pageable);
        }
        if (branchId != null) {
            return saleRepository.findByTenantIdAndBranchId(tenantId, branchId, pageable);
        }
        return saleRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public Sale get(UUID id, UUID tenantId) {
        return saleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada."));
    }

    @Transactional(readOnly = true)
    public List<SaleItem> listItems(UUID saleId) {
        return saleItemRepository.findBySaleId(saleId);
    }
}
