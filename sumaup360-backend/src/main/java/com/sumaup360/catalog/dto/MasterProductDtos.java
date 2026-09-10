package com.sumaup360.catalog.dto;

import com.sumaup360.catalog.domain.MasterProduct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MasterProductDtos {

    private MasterProductDtos() {
    }

    public record SaveMasterProductRequest(
            @Pattern(regexp = "\\d{8,14}", message = "El codigo de barras debe tener entre 8 y 14 digitos.")
            String ean,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 80) String brand,
            @Size(max = 80) String category,
            @Size(max = 80) String presentation,
            @Size(max = 600) String photoUrl,
            @Size(max = 600) String photoExternalUrl,
            @Size(max = 20) String photoSource,
            @Size(max = 600) String photoSourceUrl,
            Boolean verified,
            Boolean active,
            Set<String> rubros
    ) {
    }

    public record MasterProductView(UUID id, String ean, String name, String brand,
                                    String category, String presentation, String photoUrl,
                                    String photoExternalUrl, String photoSource, String photoSourceUrl,
                                    boolean verified, boolean active, List<String> rubros) {
        public static MasterProductView from(MasterProduct p) {
            return new MasterProductView(p.getId(), p.getEan(), p.getName(), p.getBrand(),
                    p.getCategory(), p.getPresentation(), p.getPhotoUrl(), p.getPhotoExternalUrl(),
                    p.getPhotoSource(), p.getPhotoSourceUrl(),
                    p.isVerified(), p.isActive(), p.getRubros().stream().sorted().toList());
        }
    }

    /** Vista para el tenant (sin campos internos de curacion/procedencia). */
    public record CatalogProductView(UUID id, String ean, String name, String brand,
                                     String category, String presentation, String photoUrl,
                                     String photoExternalUrl) {
        public static CatalogProductView from(MasterProduct p) {
            return new CatalogProductView(p.getId(), p.getEan(), p.getName(), p.getBrand(),
                    p.getCategory(), p.getPresentation(), p.getPhotoUrl(), p.getPhotoExternalUrl());
        }
    }
}
