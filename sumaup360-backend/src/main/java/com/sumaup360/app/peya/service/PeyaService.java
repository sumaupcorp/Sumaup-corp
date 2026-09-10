package com.sumaup360.app.peya.service;

import com.sumaup360.app.feature.Feature;
import com.sumaup360.app.feature.FeatureAccessService;
import com.sumaup360.app.peya.domain.PeyaUpload;
import com.sumaup360.app.peya.enums.PeyaStatus;
import com.sumaup360.app.peya.repository.PeyaUploadRepository;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.taxi.domain.AttachedFile;
import com.sumaup360.app.taxi.repository.AttachedFileRepository;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Repartidor (Premium): sube su PDF mensual de ventas y ve sus reportes. */
@Service
public class PeyaService {

    private static final String ENTITY = "PEYA_UPLOAD";

    private final PeyaUploadRepository repository;
    private final PersonProfileRepository profileRepository;
    private final AttachedFileRepository attachedRepository;
    private final FeatureAccessService featureAccess;

    public PeyaService(PeyaUploadRepository repository, PersonProfileRepository profileRepository,
                       AttachedFileRepository attachedRepository, FeatureAccessService featureAccess) {
        this.repository = repository;
        this.profileRepository = profileRepository;
        this.attachedRepository = attachedRepository;
        this.featureAccess = featureAccess;
    }

    @Transactional
    public PeyaUpload create(UUID userId, String periodo, String pdfUrl, String observacion) {
        if (!featureAccess.check(userId, Feature.PEYA_UPLOAD_PDF).allowed()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Funcion Premium. Activa tu plan para subir tus ventas de Peya.");
        }
        PeyaUpload u = new PeyaUpload();
        u.setUserId(userId);
        u.setPeriodo(periodo);
        u.setPdfUrl(pdfUrl);
        u.setObservacionUsuario(observacion);
        u.setEstado(PeyaStatus.PDF_SUBIDO);
        profileRepository.findByUserId(userId).ifPresent(p -> u.setRuc(p.getRuc()));
        return repository.save(u);
    }

    @Transactional(readOnly = true)
    public List<PeyaUpload> myUploads(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** Archivos resultado (reporte/declaracion) de una carga propia del usuario. */
    @Transactional(readOnly = true)
    public List<AttachedFile> myFiles(UUID userId, UUID uploadId) {
        PeyaUpload u = repository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Carga no encontrada."));
        if (!u.getUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No tienes acceso a esta carga.");
        }
        return attachedRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY, uploadId);
    }
}

