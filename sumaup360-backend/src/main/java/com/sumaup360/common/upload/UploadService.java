package com.sumaup360.common.upload;

import com.sumaup360.common.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda imágenes subidas en el disco del propio servidor (VPS) y devuelve su URL pública.
 * Reemplaza la dependencia de Firebase Storage para fotos de producto. Los archivos se sirven
 * de forma pública en /uploads/** (ver WebMvcConfig + SecurityConfig).
 */
@Service
public class UploadService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;

    private final Path baseDir;
    private final String publicBase;

    public UploadService(@Value("${app.uploads.dir:uploads}") String dir,
                         @Value("${app.uploads.public-base:}") String publicBase) {
        this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
        this.publicBase = publicBase == null ? "" : publicBase.replaceAll("/+$", "");
    }

    /** Guarda la imagen bajo {subfolder}/ y devuelve la URL (absoluta si hay public-base, o relativa). */
    public String storeImage(String subfolder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo esta vacio.");
        }
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new BadRequestException("El archivo debe ser una imagen.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("La imagen no debe superar los 5MB.");
        }
        String safeSub = subfolder.replaceAll("[^a-zA-Z0-9/_-]", "");
        Path dir = baseDir.resolve(safeSub).normalize();
        if (!dir.startsWith(baseDir)) {
            throw new BadRequestException("Ruta de subida invalida.");
        }
        String name = UUID.randomUUID() + extensionFor(file);
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BadRequestException("No se pudo guardar la imagen.");
        }
        String rel = "/uploads/" + safeSub + "/" + name;
        return publicBase.isBlank() ? rel : publicBase + rel;
    }

    private static String extensionFor(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null) {
            switch (ct) {
                case "image/jpeg": return ".jpg";
                case "image/png": return ".png";
                case "image/webp": return ".webp";
                case "image/gif": return ".gif";
                default: break;
            }
        }
        String n = file.getOriginalFilename();
        if (n != null && n.contains(".")) {
            String e = n.substring(n.lastIndexOf('.')).toLowerCase();
            if (e.matches("\\.[a-z0-9]{2,5}")) return e;
        }
        return ".img";
    }
}
