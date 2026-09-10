package com.sumaup360.erp.customorder.service;

import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.customorder.domain.CustomOrder;
import com.sumaup360.erp.customorder.dto.CustomOrderDtos.CreateCustomOrderRequest;
import com.sumaup360.erp.customorder.dto.CustomOrderDtos.CustomOrderResponse;
import com.sumaup360.erp.customorder.dto.CustomOrderDtos.UpdateCustomOrderRequest;
import com.sumaup360.erp.customorder.enums.CustomOrderStatus;
import com.sumaup360.erp.customorder.repository.CustomOrderRepository;
import com.sumaup360.erp.domain.Customer;
import com.sumaup360.erp.repository.CustomerRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Pedidos por encargo (modulo custom-orders). */
@Service
public class CustomOrderService {

    private final CustomOrderRepository customOrderRepository;
    private final CustomerRepository customerRepository;
    private final BranchRepository branchRepository;

    public CustomOrderService(CustomOrderRepository customOrderRepository,
                              CustomerRepository customerRepository,
                              BranchRepository branchRepository) {
        this.customOrderRepository = customOrderRepository;
        this.customerRepository = customerRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public CustomOrderResponse create(UUID tenantId, CreateCustomOrderRequest req) {
        requireBranch(tenantId, req.branchId());
        Customer customer = customerRepository.findByIdAndTenantId(req.customerId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado."));
        CustomOrder o = new CustomOrder();
        o.setTenantId(tenantId);
        o.setBranchId(req.branchId());
        o.setCustomerId(req.customerId());
        o.setDescription(req.description().trim());
        o.setDeliveryAt(req.deliveryAt());
        o.setTotalAmount(req.totalAmount());
        o.setAdvanceAmount(req.advanceAmount() != null ? req.advanceAmount() : BigDecimal.ZERO);
        o.setNotes(req.notes());
        return CustomOrderResponse.from(customOrderRepository.save(o), customer.getName());
    }

    @Transactional(readOnly = true)
    public List<CustomOrderResponse> list(UUID tenantId, UUID branchId, CustomOrderStatus status) {
        requireBranch(tenantId, branchId);
        List<CustomOrder> orders = (status == null)
                ? customOrderRepository.findByTenantIdAndBranchIdOrderByDeliveryAtAsc(tenantId, branchId)
                : customOrderRepository.findByTenantIdAndBranchIdAndStatusOrderByDeliveryAtAsc(
                        tenantId, branchId, status);
        Map<UUID, String> names = customerRepository.findAllById(
                        orders.stream().map(CustomOrder::getCustomerId).distinct().toList())
                .stream().collect(Collectors.toMap(Customer::getId, Customer::getName, (a, b) -> a));
        return orders.stream()
                .map(o -> CustomOrderResponse.from(o, names.get(o.getCustomerId())))
                .toList();
    }

    @Transactional
    public CustomOrderResponse update(UUID tenantId, UUID id, UpdateCustomOrderRequest req) {
        CustomOrder o = requireOrder(tenantId, id);
        if (req.description() != null && !req.description().isBlank()) o.setDescription(req.description().trim());
        if (req.deliveryAt() != null) o.setDeliveryAt(req.deliveryAt());
        if (req.totalAmount() != null) o.setTotalAmount(req.totalAmount());
        if (req.advanceAmount() != null) o.setAdvanceAmount(req.advanceAmount());
        if (req.notes() != null) o.setNotes(req.notes());
        return withName(customOrderRepository.save(o));
    }

    @Transactional
    public CustomOrderResponse updateStatus(UUID tenantId, UUID id, CustomOrderStatus status) {
        CustomOrder o = requireOrder(tenantId, id);
        o.setStatus(status);
        return withName(customOrderRepository.save(o));
    }

    private CustomOrderResponse withName(CustomOrder o) {
        String name = customerRepository.findByIdAndTenantId(o.getCustomerId(), o.getTenantId())
                .map(Customer::getName).orElse(null);
        return CustomOrderResponse.from(o, name);
    }

    private CustomOrder requireOrder(UUID tenantId, UUID id) {
        return customOrderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado."));
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }
}
