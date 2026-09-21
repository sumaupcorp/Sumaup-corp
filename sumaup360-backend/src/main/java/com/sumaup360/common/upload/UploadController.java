package com.sumaup360.common.upload;

import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** Subida de imágenes al servidor (VPS). Devuelve la URL pública del archivo guardado. */
@RestController
@RequestMapping("/api/v1/uploads")
@Tag(name = "Subidas", description = "Subida de imagenes al servidor (fotos de producto)")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    public record UploadResponse(String url) {
    }

    @PostMapping(value = "/product-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Sube la foto de un producto y devuelve su URL (requiere product:manage)")
    public UploadResponse productPhoto(@RequestPart("file") MultipartFile file) {
        UUID tenantId = SecurityUtils.requireCurrentTenant();
        return new UploadResponse(uploadService.storeImage("products/" + tenantId, file));
    }

    /** Foto del catalogo maestro (staff, backoffice). No usa Firebase Storage. */
    @PostMapping(value = "/catalog-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('catalog:product:manage')")
    @Operation(summary = "Sube la foto de un producto del catalogo maestro (requiere catalog:product:manage)")
    public UploadResponse catalogPhoto(@RequestPart("file") MultipartFile file) {
        return new UploadResponse(uploadService.storeImage("catalog", file));
    }
}
