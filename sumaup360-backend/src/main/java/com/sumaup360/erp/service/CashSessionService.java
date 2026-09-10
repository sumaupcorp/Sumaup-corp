package com.sumaup360.erp.service;

import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.domain.CashMovement;
import com.sumaup360.erp.domain.CashSession;
import com.sumaup360.erp.domain.Sale;
import com.sumaup360.erp.repository.CashMovementRepository;
import com.sumaup360.erp.repository.CashSessionRepository;
import com.sumaup360.erp.repository.SaleRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Caja / POS al estilo de la caja chica peruana.
 *
 * Ciclo: APERTURA (fondo inicial de sencillo) -> el dia opera (ventas por metodo de pago
 * + ingresos/salidas de efectivo) -> CIERRE con ARQUEO: el sistema calcula el efectivo
 * esperado (fondo + ventas en efectivo + ingresos - salidas), el cajero cuenta el efectivo
 * fisico y la diferencia queda registrada (sobrante o faltante) para control del dueno.
 * Las ventas con tarjeta / Yape / Plin / transferencia NO suman al efectivo: se cuadran
 * contra los vouchers y el estado de cuenta.
 */
@Service
public class CashSessionService {

    /** Categorias de salida de efectivo (gastos externos y retiros). */
    public static final Set<String> EXPENSE_CATEGORIES =
            Set.of("COMPRA", "SERVICIO", "PROVEEDOR", "MOVILIDAD", "RETIRO", "REMESA", "OTRO");
    /** Categorias de ingreso de efectivo distinto de ventas. */
    public static final Set<String> INCOME_CATEGORIES =
            Set.of("SENCILLO", "COBRANZA", "OTRO");

    /** Resumen vivo de la caja: lo que el cajero ve durante el dia y usa para el arqueo. */
    public record CashSummary(
            CashSession session,
            String openedByName,
            BigDecimal salesTotal,
            long salesCount,
            BigDecimal cashSales,
            Map<String, BigDecimal> salesByMethod,
            BigDecimal incomesTotal,
            BigDecimal expensesTotal,
            BigDecimal expectedCash,
            List<CashMovement> movements
    ) {
    }

    private final CashSessionRepository sessionRepository;
    private final CashMovementRepository movementRepository;
    private final SaleRepository saleRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;

    public CashSessionService(CashSessionRepository sessionRepository,
                              CashMovementRepository movementRepository,
                              SaleRepository saleRepository,
                              BranchRepository branchRepository,
                              AppUserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.movementRepository = movementRepository;
        this.saleRepository = saleRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CashSession open(UUID tenantId, UUID branchId, BigDecimal openingAmount, UUID userId) {
        requireBranch(tenantId, branchId);
        sessionRepository.findByBranchIdAndStatus(branchId, "OPEN").ifPresent(s -> {
            throw new ConflictException("Ya hay una caja abierta en esta sucursal.");
        });
        CashSession s = new CashSession();
        s.setTenantId(tenantId);
        s.setBranchId(branchId);
        s.setOpenedBy(userId);
        s.setOpeningAmount(openingAmount != null ? openingAmount : BigDecimal.ZERO);
        s.setStatus("OPEN");
        s.setOpenedAt(OffsetDateTime.now());
        return sessionRepository.save(s);
    }

    /** Registra un ingreso o salida de efectivo en la caja abierta. */
    @Transactional
    public CashMovement addMovement(UUID tenantId, UUID sessionId, String type, String category,
                                    String concept, BigDecimal amount, UUID userId) {
        CashSession s = requireOpenSession(tenantId, sessionId);
        if (!CashMovement.TYPE_INCOME.equals(type) && !CashMovement.TYPE_EXPENSE.equals(type)) {
            throw new BadRequestException("Tipo de movimiento invalido (INCOME o EXPENSE).");
        }
        Set<String> allowed = CashMovement.TYPE_INCOME.equals(type)
                ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
        if (category == null || !allowed.contains(category)) {
            throw new BadRequestException("Categoria invalida para el tipo de movimiento.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("El monto debe ser mayor a cero.");
        }
        if (concept == null || concept.isBlank()) {
            throw new BadRequestException("Describe el motivo del movimiento.");
        }
        CashMovement m = new CashMovement();
        m.setTenantId(tenantId);
        m.setCashSessionId(s.getId());
        m.setType(type);
        m.setCategory(category);
        m.setConcept(concept.trim());
        m.setAmount(amount);
        m.setCreatedBy(userId);
        return movementRepository.save(m);
    }

    /** Resumen de la sesion (abierta o cerrada): totales por metodo y efectivo esperado. */
    @Transactional(readOnly = true)
    public CashSummary summary(UUID tenantId, UUID sessionId) {
        CashSession s = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada."));
        return buildSummary(s);
    }

    /**
     * Cierra la caja con arqueo: calcula el efectivo esperado, guarda el contado y la
     * diferencia (sobrante/faltante). El cuadre queda en la sesion para auditoria.
     */
    @Transactional
    public CashSession close(UUID tenantId, UUID sessionId, BigDecimal countedAmount,
                             String notes, UUID userId) {
        CashSession s = requireOpenSession(tenantId, sessionId);
        if (countedAmount == null || countedAmount.signum() < 0) {
            throw new BadRequestException("Indica el efectivo contado al cierre.");
        }
        CashSummary summary = buildSummary(s);
        s.setStatus("CLOSED");
        s.setClosingAmount(countedAmount);
        s.setExpectedAmount(summary.expectedCash());
        s.setDifference(countedAmount.subtract(summary.expectedCash()));
        s.setNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        s.setClosedBy(userId);
        s.setClosedAt(OffsetDateTime.now());
        return sessionRepository.save(s);
    }

    /** Historial de cierres (arqueos) paginado, con nombre de sucursal resuelto. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.sumaup360.erp.web.dto.CashSessionDtos.CashClosureResponse> history(
            UUID tenantId, UUID branchId, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        var result = branchId != null
                ? sessionRepository.findByTenantIdAndBranchIdAndStatusOrderByClosedAtDesc(
                        tenantId, branchId, "CLOSED", pageable)
                : sessionRepository.findByTenantIdAndStatusOrderByClosedAtDesc(tenantId, "CLOSED", pageable);
        java.util.Map<UUID, String> branchNames = branchRepository.findByTenantId(tenantId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.sumaup360.tenant.domain.Branch::getId,
                        com.sumaup360.tenant.domain.Branch::getName, (a, b) -> a));
        return result.map(s -> com.sumaup360.erp.web.dto.CashSessionDtos.CashClosureResponse
                .from(s, branchNames.get(s.getBranchId())));
    }

    @Transactional(readOnly = true)
    public CashSession getOpenByBranch(UUID tenantId, UUID branchId) {
        requireBranch(tenantId, branchId);
        return sessionRepository.findByBranchIdAndStatus(branchId, "OPEN")
                .orElseThrow(() -> new ResourceNotFoundException("No hay caja abierta en esta sucursal."));
    }

    /** Usado por ventas: exige una caja abierta en la sucursal. */
    @Transactional(readOnly = true)
    public CashSession requireOpen(UUID tenantId, UUID branchId) {
        return sessionRepository.findByBranchIdAndStatus(branchId, "OPEN")
                .orElseThrow(() -> new BadRequestException(
                        "No hay caja abierta en la sucursal. Abre caja antes de vender."));
    }

    // ---- helpers ----

    private CashSummary buildSummary(CashSession s) {
        List<Sale> sales = saleRepository.findByCashSessionId(s.getId()).stream()
                .filter(v -> "COMPLETED".equals(v.getStatus()))
                .toList();
        BigDecimal salesTotal = BigDecimal.ZERO;
        BigDecimal cashSales = BigDecimal.ZERO;
        Map<String, BigDecimal> byMethod = new LinkedHashMap<>();
        for (Sale v : sales) {
            salesTotal = salesTotal.add(v.getTotal());
            String method = v.getPaymentMethod() != null ? v.getPaymentMethod() : "CASH";
            byMethod.merge(method, v.getTotal(), BigDecimal::add);
            if ("CASH".equals(method)) {
                cashSales = cashSales.add(v.getTotal());
            }
        }
        List<CashMovement> movements =
                movementRepository.findByCashSessionIdOrderByCreatedAtDesc(s.getId());
        BigDecimal incomes = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        for (CashMovement m : movements) {
            if (CashMovement.TYPE_INCOME.equals(m.getType())) {
                incomes = incomes.add(m.getAmount());
            } else {
                expenses = expenses.add(m.getAmount());
            }
        }
        BigDecimal expected = s.getOpeningAmount().add(cashSales).add(incomes).subtract(expenses);
        String openedByName = s.getOpenedBy() != null
                ? userRepository.findById(s.getOpenedBy())
                    .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
                    .orElse(null)
                : null;
        return new CashSummary(s, openedByName, salesTotal, sales.size(), cashSales,
                byMethod, incomes, expenses, expected, movements);
    }

    private CashSession requireOpenSession(UUID tenantId, UUID sessionId) {
        CashSession s = sessionRepository.findByIdAndTenantId(sessionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada."));
        if (!"OPEN".equals(s.getStatus())) {
            throw new BadRequestException("La caja ya esta cerrada.");
        }
        return s;
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }
}
