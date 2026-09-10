package com.sumaup360.app.honorarios.service;

import com.sumaup360.app.honorarios.domain.HonorarioRequest;
import com.sumaup360.app.honorarios.enums.HonorarioStatus;
import com.sumaup360.app.honorarios.repository.HonorarioRequestRepository;
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

/** Backoffice: procesamiento de solicitudes de Recibo por Honorarios (estado, adjuntar recibo). */
@Service
public class BackofficeHonorarioService {

    private static final String ENTITY = "HONORARIO_REQUEST";
    private static final String NOTIF_TYPE = "HONORARIO_REQUEST";

    private final HonorarioRequestRepository repository;
    private final AttachedFileRepository attachedRepository;
    private final NotificationService notificationService;

    public BackofficeHonorarioService(HonorarioRequestRepository repository,
                                      AttachedFileRepository attachedRepository,
                                      NotificationService notificationService) {
        this.repository = repository;
        this.attachedRepository = attachedRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<HonorarioRequest> list(String estado) {
        if (estado == null || estado.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findByEstadoOrderByCreatedAtDesc(parse(estado));
    }

    @Transactional
    public HonorarioRequest setStatus(UUID id, String estadoStr, String observacion) {
        HonorarioRequest h = require(id);
        HonorarioStatus estado = parse(estadoStr);
        h.setEstado(estado);
        if (observacion != null && !observacion.isBlank()) h.setObservacion(observacion);
        if (estado == HonorarioStatus.GENERADO) {
            h.setCompletedAt(OffsetDateTime.now());
            notificationService.create(h.getUserId(), "Recibo por honorarios generado",
                    "Tu recibo por honorarios fue generado. Ya puedes descargarlo.", NOTIF_TYPE);
        } else if (estado == HonorarioStatus.OBSERVADO) {
            notificationService.create(h.getUserId(), "Solicitud observada",
                    observacion != null ? observacion : "Tu solicitud fue observada.", NOTIF_TYPE);
        }
        return repository.save(h);
    }

    @Transactional
    public AttachedFile attach(UUID id, String fileUrl, String fileName, UUID staffId) {
        HonorarioRequest h = require(id);
        AttachedFile a = new AttachedFile();
        a.setEntityType(ENTITY);
        a.setEntityId(id);
        a.setFileUrl(fileUrl);
        a.setFileName(fileName);
        a.setUploadedBy(staffId);
        AttachedFile saved = attachedRepository.save(a);
        // El recibo principal se guarda tambien en la solicitud para acceso directo.
        h.setReciboUrl(fileUrl);
        repository.save(h);
        notificationService.create(h.getUserId(), "Recibo disponible",
                "Tienes tu recibo por honorarios listo para descargar.", NOTIF_TYPE);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> attachments(UUID id) {
        return attachedRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY, id);
    }

    private HonorarioRequest require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada."));
    }

    private static HonorarioStatus parse(String value) {
        try {
            return HonorarioStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado invalido: " + value);
        }
    }
}
