package com.sumaup360.app.taxi.service;

import com.sumaup360.app.taxi.domain.AttachedFile;
import com.sumaup360.app.taxi.domain.ReceiptRequest;
import com.sumaup360.app.taxi.enums.RequestStatus;
import com.sumaup360.app.taxi.repository.AttachedFileRepository;
import com.sumaup360.app.taxi.repository.ReceiptRequestRepository;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Backoffice: procesamiento de solicitudes de comprobante (cambiar estado, adjuntar, completar). */
@Service
public class BackofficeRequestService {

    private static final String ENTITY = "RECEIPT_REQUEST";

    private final ReceiptRequestRepository requestRepository;
    private final AttachedFileRepository attachedRepository;
    private final NotificationService notificationService;

    public BackofficeRequestService(ReceiptRequestRepository requestRepository,
                                    AttachedFileRepository attachedRepository,
                                    NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.attachedRepository = attachedRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ReceiptRequest> list(String estado) {
        if (estado == null || estado.isBlank()) {
            return requestRepository.findAllByOrderByCreatedAtDesc();
        }
        return requestRepository.findByEstadoOrderByCreatedAtDesc(parse(estado));
    }

    @Transactional
    public ReceiptRequest setStatus(UUID requestId, String estadoStr, String motivo) {
        ReceiptRequest r = require(requestId);
        RequestStatus estado = parse(estadoStr);
        r.setEstado(estado);
        if (motivo != null && !motivo.isBlank()) r.setMotivo(motivo);
        if (estado == RequestStatus.COMPLETADO) {
            r.setCompletedAt(OffsetDateTime.now());
            notificationService.create(r.getTaxistaUserId(), "Comprobante generado",
                    "Tu comprobante fue generado y enviado correctamente.", "RECEIPT_REQUEST");
        } else if (estado == RequestStatus.OBSERVADO) {
            notificationService.create(r.getTaxistaUserId(), "Solicitud observada",
                    motivo != null ? motivo : "Tu solicitud fue observada.", "RECEIPT_REQUEST");
        }
        return requestRepository.save(r);
    }

    @Transactional
    public AttachedFile attach(UUID requestId, String fileUrl, String fileName, UUID staffId) {
        ReceiptRequest r = require(requestId);
        AttachedFile a = new AttachedFile();
        a.setEntityType(ENTITY);
        a.setEntityId(requestId);
        a.setFileUrl(fileUrl);
        a.setFileName(fileName);
        a.setUploadedBy(staffId);
        AttachedFile saved = attachedRepository.save(a);
        notificationService.create(r.getTaxistaUserId(), "Archivo disponible",
                "Tienes un archivo de tu comprobante listo para descargar.", "RECEIPT_REQUEST");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AttachedFile> attachments(UUID requestId) {
        return attachedRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY, requestId);
    }

    /** Elimina un adjunto de la solicitud (el archivo en Storage lo borra el frontend). */
    @Transactional
    public void deleteAttachment(UUID requestId, UUID attachmentId) {
        AttachedFile a = attachedRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjunto no encontrado."));
        if (!ENTITY.equals(a.getEntityType()) || !requestId.equals(a.getEntityId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El adjunto no pertenece a esta solicitud.");
        }
        attachedRepository.delete(a);
    }

    private ReceiptRequest require(UUID id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada."));
    }

    private static RequestStatus parse(String value) {
        try {
            return RequestStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Estado invalido: " + value);
        }
    }
}
