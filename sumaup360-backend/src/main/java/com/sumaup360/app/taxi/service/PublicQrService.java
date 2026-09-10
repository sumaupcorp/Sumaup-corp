package com.sumaup360.app.taxi.service;

import com.sumaup360.app.taxi.domain.QrCustomer;
import com.sumaup360.app.taxi.domain.QrCode;
import com.sumaup360.app.taxi.domain.ReceiptRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.CreatePublicRequest;
import com.sumaup360.app.taxi.dto.TaxiDtos.QrPublicView;
import com.sumaup360.app.taxi.enums.ComprobanteType;
import com.sumaup360.app.taxi.enums.RequestStatus;
import com.sumaup360.app.taxi.repository.QrCustomerRepository;
import com.sumaup360.app.taxi.repository.QrCodeRepository;
import com.sumaup360.app.taxi.repository.ReceiptRequestRepository;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.billing.service.SubscriptionService;
import com.sumaup360.common.error.ApiException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Endpoints PUBLICOS del QR del taxista (sin auth). El cliente final solicita su
 * comprobante; se guarda y se notifica al taxista. La BD de clientes es global:
 * un mismo cliente (por documento) se reutiliza entre taxistas.
 */
@Service
public class PublicQrService {

    private final QrCodeRepository qrRepository;
    private final QrCustomerRepository customerRepository;
    private final ReceiptRequestRepository requestRepository;
    private final NotificationService notificationService;
    private final AppUserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public PublicQrService(QrCodeRepository qrRepository, QrCustomerRepository customerRepository,
                           ReceiptRequestRepository requestRepository, NotificationService notificationService,
                           AppUserRepository userRepository, SubscriptionService subscriptionService) {
        this.qrRepository = qrRepository;
        this.customerRepository = customerRepository;
        this.requestRepository = requestRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public boolean isValid(String token) {
        return qrRepository.findByTokenAndActiveTrue(token).isPresent();
    }

    /** Datos publicos del QR: valido + nombre del taxista (confianza, estilo Yape). */
    @Transactional(readOnly = true)
    public QrPublicView info(String token) {
        QrCode qr = qrRepository.findByTokenAndActiveTrue(token).orElse(null);
        if (qr == null) return new QrPublicView(false, null);
        String name = userRepository.findById(qr.getUserId())
                .map(u -> u.getDisplayName()).orElse(null);
        return new QrPublicView(true, name);
    }

    /**
     * Precarga por documento. Requiere un token de QR valido (evita enumeracion anonima) y
     * por privacidad solo devuelve el NOMBRE (el controller/DTO no expone contacto).
     */
    @Transactional(readOnly = true)
    public QrCustomer lookup(String token, String docNumber) {
        if (docNumber == null || docNumber.isBlank()) return null;
        if (!isValid(token)) return null;
        return customerRepository.findFirstByDocNumber(docNumber.trim()).orElse(null);
    }

    @Transactional
    public ReceiptRequest createRequest(String token, CreatePublicRequest req) {
        QrCode qr = qrRepository.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ResourceNotFoundException("Codigo no valido."));
        // El taxista debe estar activo (Premium) para poder atender la solicitud.
        if (!subscriptionService.isUserPremium(qr.getUserId())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Este taxista no esta recibiendo solicitudes por ahora.");
        }
        ComprobanteType tipo = parseTipo(req.tipo());
        validateTipoDoc(tipo, req.docType());
        validateDoc(req.docType(), req.docNumber());

        // Evita duplicados por doble envio: si ya hay una solicitud identica pendiente, la reusa.
        Optional<ReceiptRequest> dup = requestRepository
                .findFirstByQrTokenAndDocNumberAndMontoAndTipoAndEstadoOrderByCreatedAtDesc(
                        token, req.docNumber(), req.monto(), tipo, RequestStatus.PENDIENTE_TAXISTA);
        if (dup.isPresent()) {
            return dup.get();
        }

        QrCustomer customer = upsertCustomer(req);

        ReceiptRequest r = new ReceiptRequest();
        r.setTaxistaUserId(qr.getUserId());
        r.setCustomerId(customer != null ? customer.getId() : null);
        r.setQrToken(token);
        r.setTipo(tipo);
        r.setMonto(req.monto());
        r.setDocType(req.docType());
        r.setDocNumber(req.docNumber());
        r.setCustomerName(req.name());
        r.setWhatsapp(req.whatsapp());
        r.setEmail(req.email());
        r.setObservacion(req.observacion());
        r.setEstado(RequestStatus.PENDIENTE_TAXISTA);
        r = requestRepository.save(r);

        notificationService.create(qr.getUserId(), "Nueva solicitud de comprobante",
                "Un cliente solicita " + tipo.name().toLowerCase() + " por S/ " + req.monto() + ".",
                "RECEIPT_REQUEST");
        return r;
    }

    private QrCustomer upsertCustomer(CreatePublicRequest req) {
        if (req.docNumber() == null || req.docNumber().isBlank()) return null;
        Optional<QrCustomer> existing = customerRepository.findFirstByDocNumber(req.docNumber().trim());
        QrCustomer c = existing.orElseGet(QrCustomer::new);
        c.setDocType(req.docType());
        c.setDocNumber(req.docNumber().trim());
        // Cliente GLOBAL entre taxistas: desde un flujo anonimo solo se RELLENAN campos vacios;
        // nunca se sobrescriben datos ya existentes (evita que una solicitud pise/altere el PII
        // de otro). El nombre si se actualiza porque es el que ira en el comprobante.
        if (req.name() != null && !req.name().isBlank()) c.setName(req.name().trim());
        if ((c.getWhatsapp() == null || c.getWhatsapp().isBlank())
                && req.whatsapp() != null && !req.whatsapp().isBlank()) {
            c.setWhatsapp(req.whatsapp().trim());
        }
        if ((c.getEmail() == null || c.getEmail().isBlank())
                && req.email() != null && !req.email().isBlank()) {
            c.setEmail(req.email().trim());
        }
        return customerRepository.save(c);
    }

    /**
     * El comprobante determina el documento: la FACTURA exige RUC (obligatorio por SUNAT) y
     * la BOLETA exige DNI. Evita que llegue una combinacion invalida por la API publica.
     */
    private static void validateTipoDoc(ComprobanteType tipo, String docType) {
        String dt = docType == null ? "" : docType.trim().toUpperCase();
        if (tipo == ComprobanteType.FACTURA && !dt.equals("RUC")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Para una factura necesitas RUC.");
        }
        if (tipo == ComprobanteType.BOLETA && !dt.equals("DNI")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Para una boleta necesitas DNI.");
        }
    }

    /** Valida el documento: numerico y con la longitud correcta segun DNI (8) o RUC (11). */
    private static void validateDoc(String docType, String docNumber) {
        if (docNumber == null || docNumber.isBlank()) return; // opcional
        String dn = docNumber.trim();
        if (!dn.matches("\\d+")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El documento debe ser numerico.");
        }
        String dt = docType == null ? "" : docType.trim().toUpperCase();
        if (dt.equals("RUC") && dn.length() != 11) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El RUC debe tener 11 digitos.");
        }
        if (dt.equals("DNI") && dn.length() != 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El DNI debe tener 8 digitos.");
        }
    }

    private static ComprobanteType parseTipo(String value) {
        try {
            return ComprobanteType.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tipo de comprobante invalido (BOLETA o FACTURA).");
        }
    }
}
