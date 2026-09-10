package com.sumaup360.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Inicializa Firebase Admin SOLO si hay un service account disponible.
 *
 * Asi el backend arranca en local sin credenciales (Swagger y migraciones funcionan);
 * en ese caso la verificacion real de idToken queda inactiva y, si security.dev-mode=true,
 * se acepta el header X-Debug-Uid para pruebas. En produccion SIEMPRE debe haber credenciales.
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    /**
     * Devuelve el FirebaseAuth si se pudo inicializar, o null si no hay credenciales.
     * Los consumidores deben tolerar su ausencia (ver FirebaseTokenFilter).
     */
    @Bean
    public FirebaseAuth firebaseAuth(@Value("${firebase.service-account:}") String serviceAccountPath,
                                     @Value("${firebase.project-id:}") String projectId) {
        try {
            if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
                log.warn("Firebase no configurado (firebase.service-account vacio). "
                        + "Verificacion de idToken INACTIVA.");
                return null;
            }
            Path path = Path.of(serviceAccountPath);
            if (!Files.exists(path)) {
                log.warn("Service account de Firebase no encontrado en '{}'. "
                        + "Verificacion de idToken INACTIVA.", serviceAccountPath);
                return null;
            }
            try (InputStream in = Files.newInputStream(path)) {
                FirebaseOptions.Builder builder = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in));
                if (projectId != null && !projectId.isBlank()) {
                    builder.setProjectId(projectId);
                }
                FirebaseApp app = FirebaseApp.getApps().isEmpty()
                        ? FirebaseApp.initializeApp(builder.build())
                        : FirebaseApp.getInstance();
                log.info("Firebase Admin inicializado. Verificacion de idToken ACTIVA.");
                return FirebaseAuth.getInstance(app);
            }
        } catch (Exception e) {
            log.error("No se pudo inicializar Firebase Admin: {}. Verificacion INACTIVA.", e.getMessage());
            return null;
        }
    }
}
