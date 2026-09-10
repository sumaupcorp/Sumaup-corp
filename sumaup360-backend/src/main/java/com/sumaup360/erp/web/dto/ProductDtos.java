package com.sumaup360.erp.web.dto;

import com.sumaup360.catalog.domain.MasterProduct;
import com.sumaup360.erp.domain.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record CreateProductRequest(
            @NotNull UUID companyId,
            @NotBlank @Size(max = 40) String sku,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 20) String unit,
            @PositiveOrZero BigDecimal price,
            @Size(max = 80) String category,
            UUID masterProductId,
            @Size(max = 500) String photoUrl
    ) {
    }

    public record UpdateProductRequest(
            @Size(max = 160) String name,
            @Size(max = 20) String unit,
            @PositiveOrZero BigDecimal price,
            @Size(max = 80) String category,
            Boolean active,
            @Size(max = 500) String photoUrl
    ) {
    }

    /** Foto: la propia del negocio manda; si no tiene, hereda la del catalogo maestro. */
    public record ProductResponse(UUID id, String sku, String name, String unit,
                                  BigDecimal price, String category, boolean active,
                                  UUID masterProductId, String photoUrl, String photoExternalUrl) {
        public static ProductResponse from(Product p, MasterProduct master) {
            boolean ownPhoto = p.getPhotoUrl() != null && !p.getPhotoUrl().isBlank();
            return new ProductResponse(p.getId(), p.getSku(), p.getName(), p.getUnit(),
                    p.getPrice(), p.getCategory(), p.isActive(), p.getMasterProductId(),
                    ownPhoto ? p.getPhotoUrl() : (master != null ? master.getPhotoUrl() : null),
                    ownPhoto ? null : (master != null ? master.getPhotoExternalUrl() : null));
        }
    }
}
