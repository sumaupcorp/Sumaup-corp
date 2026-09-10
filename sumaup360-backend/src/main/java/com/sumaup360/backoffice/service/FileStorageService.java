package com.sumaup360.backoffice.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.sumaup360.common.error.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.concurrent.TimeUnit;

/**
 * Acceso a archivos en Firebase Storage. Genera signed URLs V4 firmados LOCALMENTE con la
 * llave del service account (no requiere token OAuth ni salida de red del backend); el
 * navegador descarga el objeto directo desde Google Storage. El bucket es privado.
 */
@Service
public class FileStorageService {

    private final String serviceAccountPath;
    private final String bucket;
    private volatile Storage storage;

    public FileStorageService(@Value("${firebase.service-account:}") String serviceAccountPath,
                              @Value("${firebase.storage-bucket:}") String bucket) {
        this.serviceAccountPath = serviceAccountPath;
        this.bucket = bucket;
    }

    /** URL firmada (V4) de corta duracion para leer un objeto privado. */
    public String signedReadUrl(String objectPath, int minutes) {
        if (bucket == null || bucket.isBlank()) {
            throw new BadRequestException("Firebase Storage no esta configurado (firebase.storage-bucket).");
        }
        try {
            BlobInfo blob = BlobInfo.newBuilder(bucket, objectPath).build();
            return storage().signUrl(blob, minutes, TimeUnit.MINUTES, Storage.SignUrlOption.withV4Signature()).toString();
        } catch (Exception e) {
            throw new BadRequestException("No se pudo generar el enlace del archivo: " + e.getMessage());
        }
    }

    private Storage storage() throws Exception {
        if (storage == null) {
            synchronized (this) {
                if (storage == null) {
                    GoogleCredentials creds;
                    try (FileInputStream in = new FileInputStream(serviceAccountPath)) {
                        creds = GoogleCredentials.fromStream(in);
                    }
                    storage = StorageOptions.newBuilder().setCredentials(creds).build().getService();
                }
            }
        }
        return storage;
    }
}
