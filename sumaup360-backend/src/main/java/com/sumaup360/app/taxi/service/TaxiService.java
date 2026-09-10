package com.sumaup360.app.taxi.service;

import com.sumaup360.app.feature.Feature;
import com.sumaup360.app.feature.FeatureAccessService;
import com.sumaup360.app.taxi.domain.AttachedFile;
import com.sumaup360.app.taxi.domain.QrCode;
import com.sumaup360.app.taxi.domain.ReceiptRequest;
import com.sumaup360.app.taxi.enums.RequestStatus;
import com.sumaup360.app.taxi.repository.AttachedFileRepository;
import com.sumaup360.app.taxi.repository.QrCodeRepository;
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

    private final QrCodeRepository qrRepository;
    private final ReceiptRequestRepository requestRepository;
    private final AttachedFileRepository attachedRepository;
    private final FeatureAccessService featureAccess;

    public TaxiService(QrCodeRepository qrRepository, ReceiptRequestRepository requestRepository,
                       AttachedFileRepository attachedRepository, FeatureAccessService featureAccess) {
        this.qrRepository = qrRepository;
        this.requestRepository = requestRepository;
        this.attachedRepository = attachedRepository;
        this.featureAccess = featureAccess;
    }

    /** Token del QR del taxista (lo crea si no existe). Requiere Premium. */
    @Transactional
    public String getOrCreateQrToken(UUID userId) {
        requirePremium(userId, Feature.TAXI_QR);
        QrCode qr = qrRepository.findFirstByUserId(userId).orElseGet(() -> {
            QrCode n = new QrCode();
            n.setUserId(userId);
            n.setToken(UUID.randomUUID().toString().replace("-", ""));
            n.setActive(true);
            return qrRepository.save(n);
        });
        return qr.getToken();
    }

    /** Regenera el token del QR del taxista (invalida el anterior). Util si le hacen spam. */
    @Transactional
    public String regenerateQrToken(UUID userId) {
        requirePremium(userId, Feature.TAXI_QR);
        QrCode qr = qrRepository.findFirstByUserId(userId).orElseGet(() -> {
            QrCode n = new QrCode();
            n.setUserId(userId);
            return n;
        });
        qr.setToken(UUID.randomUUID().toString().replace("-", ""));
        qr.setActive(true);
        return qrRepository.save(qr).getToken();
    }

    @Transactional(readOnly = true)
    public List<ReceiptRequest> myRequests(UUID userId) {
        return requestRepository.findByTaxistaUserIdOrderByCreatedAtDesc(userId);
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
