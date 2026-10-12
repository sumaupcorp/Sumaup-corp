package com.sumaup360.app.taxi.service;

import com.sumaup360.app.feature.Feature;
import com.sumaup360.app.feature.FeatureAccessService;
import com.sumaup360.app.taxi.domain.AttachedFile;
import com.sumaup360.app.taxi.domain.ReceiptRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.CreateCorporateRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.UpdateCorporateRequest;
import com.sumaup360.app.taxi.enums.ComprobanteType;
import com.sumaup360.app.taxi.enums.RequestStatus;
import com.sumaup360.app.taxi.repository.AttachedFileRepository;
import com.sumaup360.app.taxi.repository.ReceiptRequestRepository;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Acciones del taxista (Premium): generar QR y gestionar solicitudes de comprobante. */
@Service
public class TaxiService {

    private static final String ATTACH_ENTITY = "RECEIPT_REQUEST";

    private final ReceiptRequestRepository requestRepository;
    private final AttachedFileRepository attachedRepository;
    private final FeatureAccessService featureAccess;

    public TaxiService(ReceiptRequestRepository requestRepository,
                       AttachedFileRepository attachedRepository, FeatureAccessService featureAccess) {
        this.requestRepository = requestRepository;
        this.attachedRepository = attachedRepository;
        this.featureAccess = featureAccess;
    }

    /** Token del QR del taxista (es el mismo userId). Requiere Premium. */
    @Transactional(readOnly = true)
    public String getQrToken(UUID userId) {
        requirePremium(userId, Feature.TAXI_QR);
        return userId.toString();
    }

    @Transactional(readOnly = true)
    public List<ReceiptRequest> myRequests(UUID userId) {
        return myRequests(userId, null);
    }

    @Transactional(readOnly = true)
    public List<ReceiptRequest> myRequests(UUID userId, String origen) {
        if (origen != null && !origen.isBlank()) {
            return requestRepository.findByTaxistaUserIdAndOrigenOrderByCreatedAtDesc(userId, origen.trim().toUpperCase());
        }
        return requestRepository.findByTaxistaUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public ReceiptRequest createCorporateRequest(UUID userId, CreateCorporateRequest req) {
        requirePremium(userId, Feature.TAXI_COMPROBANTES);

        ReceiptRequest r = new ReceiptRequest();
        r.setTaxistaUserId(userId);
        r.setOrigen("CORPORATIVO");
        r.setTipo(ComprobanteType.FACTURA);
        r.setMonto(req.monto());
        r.setDocType("RUC");
        r.setDocNumber(req.docNumber() != null ? req.docNumber().trim() : null);
        r.setCustomerName(req.name() != null ? req.name().trim() : null);
        r.setWhatsapp(req.whatsapp() != null ? req.whatsapp().trim() : null);
        r.setEmail(req.email() != null && !req.email().isBlank() ? req.email().trim() : null);
        r.setObservacion(req.observacion() != null && !req.observacion().isBlank() ? req.observacion().trim() : null);
        // Directo a backoffice: el taxista ya la envio el mismo, no requiere confirmacion
        r.setEstado(RequestStatus.PENDIENTE_BACKOFFICE);

        return requestRepository.save(r);
    }

    @Transactional
    public ReceiptRequest updateCorporateRequest(UUID userId, UUID requestId, UpdateCorporateRequest req) {
        requirePremium(userId, Feature.TAXI_COMPROBANTES);

        ReceiptRequest r = requestRepository.findByIdAndTaxistaUserId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada."));

        if (!"CORPORATIVO".equalsIgnoreCase(r.getOrigen())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se pueden editar solicitudes corporativas.");
        }

        if (r.getEstado() == RequestStatus.COMPLETADO || r.getEstado() == RequestStatus.CANCELADO) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede editar una solicitud completada o cancelada.");
        }

        r.setMonto(req.monto());
        r.setDocNumber(req.docNumber() != null ? req.docNumber().trim() : null);
        r.setCustomerName(req.name() != null ? req.name().trim() : null);
        r.setWhatsapp(req.whatsapp() != null ? req.whatsapp().trim() : null);
        r.setEmail(req.email() != null && !req.email().isBlank() ? req.email().trim() : null);
        r.setObservacion(req.observacion() != null && !req.observacion().isBlank() ? req.observacion().trim() : null);

        return requestRepository.save(r);
    }

    /** Adjuntos de una solicitud propia del taxista (verifica que le pertenezca). */
    @Transactional(readOnly = true)
    public List<AttachedFile> myRequestAttachments(UUID userId, UUID requestId) {
        requestRepository.findByIdAndTaxistaUserId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada."));
        return attachedRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ATTACH_ENTITY, requestId);
    }

    @Transactional
    public ReceiptRequest confirm(UUID userId, UUID requestId) {
        ReceiptRequest r = ownPending(userId, requestId);
        r.setEstado(RequestStatus.CONFIRMADO_TAXISTA);
        return requestRepository.save(r);
    }

    @Transactional
    public ReceiptRequest editMonto(UUID userId, UUID requestId, BigDecimal nuevoMonto, String motivo) {
        if (nuevoMonto == null || nuevoMonto.signum() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa un monto valido.");
        }
        ReceiptRequest r = ownPending(userId, requestId);
        r.setMontoEditado(nuevoMonto);
        r.setMotivo(motivo);
        r.setEstado(RequestStatus.MONTO_EDITADO_CONFIRMADO);
        return requestRepository.save(r);
    }

    @Transactional
    public ReceiptRequest reject(UUID userId, UUID requestId, String motivo) {
        ReceiptRequest r = ownPending(userId, requestId);
        r.setMotivo(motivo);
        r.setEstado(RequestStatus.RECHAZADO_TAXISTA);
        return requestRepository.save(r);
    }

    private ReceiptRequest ownPending(UUID userId, UUID requestId) {
        requirePremium(userId, Feature.TAXI_COMPROBANTES);
        ReceiptRequest r = requestRepository.findByIdAndTaxistaUserId(requestId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada."));
        if (r.getEstado() != RequestStatus.PENDIENTE_TAXISTA) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta solicitud ya fue gestionada.");
        }
        return r;
    }

    private void requirePremium(UUID userId, Feature feature) {
        FeatureAccessService.Access a = featureAccess.check(userId, feature);
        if (!a.allowed()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Funcion Premium. Activa tu plan para usar tus comprobantes.");
        }
    }
}
