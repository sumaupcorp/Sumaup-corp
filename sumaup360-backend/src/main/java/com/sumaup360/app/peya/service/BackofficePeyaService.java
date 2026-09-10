package com.sumaup360.app.peya.service;

import com.sumaup360.app.peya.domain.PeyaUpload;
import com.sumaup360.app.peya.enums.PeyaStatus;
import com.sumaup360.app.peya.repository.PeyaUploadRepository;
import com.sumaup360.app.taxi.domain.AttachedFile;
import com.sumaup360.app.taxi.repository.AttachedFileRepository;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Backoffice: procesamiento de cargas Peya (estado, NPS, adjuntos del reporte). */
@Service
public class BackofficePeyaService {

    private static final String ENTITY = "PEYA_UPLOAD";

    private final PeyaUploadRepository repository;
    private final AttachedFileRepository attachedRepository;
    private final NotificationService notificationService;

    public BackofficePeyaService(PeyaUploadRepository repository, AttachedFileRepository attachedRepository,
                                 NotificationService notificationService) {
        this.repository = repository;
        this.attachedRepository = attachedRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PeyaUpload> list(String estado) {
        if (estado == null || estado.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findByEstadoOrderByCreatedAtDesc(parse(estado));
    }

    @Transactional
    public PeyaUpload setStatus(UUID id, String estadoStr, String observacion) {
        PeyaUpload u = require(id);
        PeyaStatus estado = parse(estadoStr);
        u.setEstado(estado);
        if (observacion != null && !observacion.isBlank()) u.setObservacionBackoffice(observacion);
        if (estado == PeyaStatus.COMPLETADO) {
            u.setCompletedAt(OffsetDateTime.now());
            notificationService.create(u.getUserId(), "Tu reporte esta listo",
                    "Tu reporte y declaracion de Peya del periodo " + u.getPeriodo() + " ya estan disponibles.", "PEYA");
        } else if (estado == PeyaStatus.OBSERVADO) {
            notificationService.create(u.getUserId(), "Carga observada",
                    observacion != null ? observacion : "Tu carga de Peya fue observada.", "PEYA");
        }
        return repository.save(u);
    }

    @Transactional
    public PeyaUpload setNps(UUID id, String codigoNps) {
        PeyaUpload u = require(id);
        u.setCodigoNps(codigoNps);
        notificationService.create(u.getUserId(), "Codigo NPS generado",
                "Tu codigo NPS del periodo " + u.getPeriodo() + " ya esta disponible.", "PEYA");
        return repository.save(u);
    }

    @Transactional
    public AttachedFile attach(UUID id, String fileUrl, String fileName, UUID staffId) {
        PeyaUpload u = require(id);
        AttachedFile a = new AttachedFile();
        a.setEntityType(ENTITY);
        a.setEntityId(id);
        a.setFileUrl(fileUrl);
        a.setFileName(fileName);
        a.setUploadedBy(staffId);
        AttachedFile saved = attachedRepository.save(a);
        notificationService.create(u.getUserId(), "Archivo disponible",
                "Tienes un archivo de tu reporte de Peya listo para descargar.", "PEYA");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> attachments(UUID id) {
        return attachedRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY, id);
    }

    private PeyaUpload require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Carga no encontrada."));
    }

    private static PeyaStatus parse(String value) {
        try {
            return PeyaStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado invalido: " + value);
        }
    }
}
