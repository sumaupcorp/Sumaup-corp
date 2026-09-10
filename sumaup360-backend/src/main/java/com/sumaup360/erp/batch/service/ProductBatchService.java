package com.sumaup360.erp.batch.service;

import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.batch.domain.ProductBatch;
import com.sumaup360.erp.batch.dto.BatchDtos.BatchResponse;
import com.sumaup360.erp.batch.dto.BatchDtos.CreateBatchRequest;
import com.sumaup360.erp.batch.dto.BatchDtos.UpdateBatchRequest;
import com.sumaup360.erp.batch.enums.BatchStatus;
import com.sumaup360.erp.batch.repository.ProductBatchRepository;
import com.sumaup360.erp.domain.Product;
import com.sumaup360.erp.repository.ProductRepository;
import com.sumaup360.tenant.repository.BranchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Lotes y vencimientos por producto y sucursal (modulo batch-expiry). */
@Service
public class ProductBatchService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    public ProductBatchService(ProductBatchRepository batchRepository,
                               ProductRepository productRepository,
                               BranchRepository branchRepository) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public BatchResponse create(UUID tenantId, CreateBatchRequest req) {
        requireBranch(tenantId, req.branchId());
        Product product = requireProduct(tenantId, req.productId());
        batchRepository.findByTenantIdAndBranchIdAndProductIdAndBatchCodeIgnoreCase(
                        tenantId, req.branchId(), req.productId(), req.batchCode().trim())
                .ifPresent(b -> {
                    throw new ConflictException("Ya existe el lote '" + req.batchCode() + "' para este producto.");
                });
        ProductBatch b = new ProductBatch();
        b.setTenantId(tenantId);
        b.setBranchId(req.branchId());
        b.setProductId(req.productId());
        b.setBatchCode(req.batchCode().trim());
        b.setExpiryDate(req.expiryDate());
        b.setQuantity(req.quantity());
        b.setNotes(req.notes());
        return BatchResponse.from(batchRepository.save(b), product.getName());
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> list(UUID tenantId, UUID branchId, UUID productId) {
        requireBranch(tenantId, branchId);
        List<ProductBatch> batches = (productId == null)
                ? batchRepository.findByTenantIdAndBranchIdOrderByExpiryDateAsc(tenantId, branchId)
                : batchRepository.findByTenantIdAndBranchIdAndProductIdOrderByExpiryDateAsc(
                        tenantId, branchId, productId);
        return withProductNames(batches);
    }

    /** Lotes activos que vencen en los proximos {days} dias (o ya vencidos). */
    @Transactional(readOnly = true)
    public List<BatchResponse> expiring(UUID tenantId, int days) {
        LocalDate until = LocalDate.now().plusDays(Math.max(0, days));
        return withProductNames(batchRepository
                .findByTenantIdAndStatusAndExpiryDateLessThanEqualOrderByExpiryDateAsc(
                        tenantId, BatchStatus.ACTIVE, until));
    }

    @Transactional
    public BatchResponse update(UUID tenantId, UUID id, UpdateBatchRequest req) {
        ProductBatch b = batchRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote no encontrado."));
        if (req.quantity() != null) b.setQuantity(req.quantity());
        if (req.expiryDate() != null) b.setExpiryDate(req.expiryDate());
        if (req.status() != null) b.setStatus(req.status());
        if (req.notes() != null) b.setNotes(req.notes());
        String productName = productRepository.findByIdAndTenantId(b.getProductId(), tenantId)
                .map(Product::getName).orElse(null);
        return BatchResponse.from(batchRepository.save(b), productName);
    }

    private List<BatchResponse> withProductNames(List<ProductBatch> batches) {
        Map<UUID, String> names = productRepository.findAllById(
                        batches.stream().map(ProductBatch::getProductId).distinct().toList())
                .stream().collect(Collectors.toMap(Product::getId, Product::getName, (a, c) -> a));
        return batches.stream()
                .map(b -> BatchResponse.from(b, names.get(b.getProductId())))
                .toList();
    }

    private void requireBranch(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada."));
    }

    private Product requireProduct(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado."));
    }
}
