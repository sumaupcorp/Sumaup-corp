package com.sumaup360.config;

import com.sumaup360.audit.AuditInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** Registra el interceptor de auditoria y el serving de archivos subidos (/uploads/**). */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuditInterceptor auditInterceptor;
    private final String uploadsDir;

    public WebMvcConfig(AuditInterceptor auditInterceptor,
                        @Value("${app.uploads.dir:uploads}") String uploadsDir) {
        this.auditInterceptor = auditInterceptor;
        this.uploadsDir = uploadsDir;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(auditInterceptor).addPathPatterns("/api/v1/**");
    }

    /** Sirve las imagenes subidas desde el disco del servidor. La lectura es publica. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(uploadsDir).toAbsolutePath().normalize() + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
