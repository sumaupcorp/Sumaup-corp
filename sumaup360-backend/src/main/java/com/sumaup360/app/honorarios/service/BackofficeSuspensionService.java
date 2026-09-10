package com.sumaup360.app.honorarios.service;

import com.sumaup360.app.honorarios.domain.SuspensionRequest;
import com.sumaup360.app.honorarios.enums.SuspensionStatus;
import com.sumaup360.app.honorarios.repository.SuspensionRequestRepository;
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

/** Backoffice: tramitacion de solicitudes de Suspension de 4ta (estado, adjuntar constancia). */
@Service
public class BackofficeSuspensionService {

    private static final String ENTITY = "SUSPENSION_REQUEST";
    private static final String NOTIF_TYPE = "SUSPENSION_REQUEST";

    private final SuspensionRequestRepository repository;
    private final AttachedFileRepository attachedRepository;
    private final NotificationService notificationService;

    public BackofficeSuspensionService(SuspensionRequestRepository repository,
                                       AttachedFileRepository attachedRepository,
                                       NotificationService notificationService) {
        this.repository = repository;
        this.attachedRepository = attachedRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<SuspensionRequest> list(String estado) {
        if (estado == null || estado.isBlank()) {
            return repository.findAllByOrderByCreatedAtDesc();
        }
        return repository.findByEstadoOrderByCreatedAtDesc(parse(estado));
    }

    @Transactional
    public SuspensionRequest setStatus(UUID id, String estadoStr, String observacion) {
        SuspensionRequest s = require(id);
        SuspensionStatus estado = parse(estadoStr);
        s.setEstado(estado);
        if (observacion != null && !observacion.isBlank()) s.setObservacion(observacion);
        if (estado == SuspensionStatus.TRAMITADA) {
            s.setCompletedAt(OffsetDateTime.now());
            notificationService.create(s.getUserId(), "Suspension de 4ta tramitada",
                    "Tu suspension de 4ta categoria fue tramitada. Revisa tu constancia.", NOTIF_TYPE);
        } else if (estado == SuspensionStatus.OBSERVADA) {
            notificationService.create(s.getUserId(), "Solicitud observada",
                    observacion != null ? observacion : "Tu solicitud fue observada.", NOTIF_TYPE);
        }
        return repository.save(s);
    }

    @Transactional
    public AttachedFile attach(UUID id, String fileUrl, String fileName, UUID staffId) {
        SuspensionRequest s = require(id);
        AttachedFile a = new AttachedFile();
        a.setEntityType(ENTITY);
        a.setEntityId(id);
        a.setFileUrl(fileUrl);
        a.setFileName(fileName);
        a.setUploadedBy(staffId);
        AttachedFile saved = attachedRepository.save(a);
        s.setConstanciaUrl(fileUrl);
        repository.save(s);
        notificationService.create(s.getUserId(), "Constancia disponible",
                "Tienes la constancia de tu suspension de 4ta lista para descargar.", NOTIF_TYPE);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> attachments(UUID id) {
        return attachedRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY, id);
    }

    private SuspensionRequest require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada."));
    }

    private static SuspensionStatus parse(String value) {
        try {
            return SuspensionStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado invalido: " + value);
        }
    }
}
