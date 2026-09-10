package com.sumaup360.backoffice.service;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.domain.Receipt;
import com.sumaup360.app.enums.ReceiptStatus;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.app.repository.ReceiptRepository;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.backoffice.dto.BackofficeDtos.ReceiptView;
import com.sumaup360.backoffice.dto.BackofficeDtos.RevealSolResponse;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.service.EventNotificationService;
import com.sumaup360.security.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Intake y detalle fiscal de recibos en el Backoffice (cross-user). Los contadores procesan
 * los recibos que suben las personas (app.receipt), ven los datos fiscales del usuario
 * (RUC, Clave SOL) y registran la declaracion en SIRE y en SUNAT.
 */
@Service
public class BackofficeReceiptService {

    private static final Logger log = LoggerFactory.getLogger(BackofficeReceiptService.class);

    private final ReceiptRepository receiptRepository;
    private final PersonProfileRepository profileRepository;
    private final AppUserRepository userRepository;
    private final CryptoService crypto;
    private final FileStorageService fileStorage;
    private final EventNotificationService eventNotificationService;

    public BackofficeReceiptService(ReceiptRepository receiptRepository,
                                    PersonProfileRepository profileRepository,
                                    AppUserRepository userRepository,
                                    CryptoService crypto,
                                    FileStorageService fileStorage,
                                    EventNotificationService eventNotificationService) {
        this.receiptRepository = receiptRepository;
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.crypto = crypto;
        this.fileStorage = fileStorage;
        this.eventNotificationService = eventNotificationService;
    }

    @Transactional(readOnly = true)
    public List<ReceiptView> listByStatus(ReceiptStatus status) {
        return receiptRepository.findByStatusOrderByCreatedAtAsc(
                        status != null ? status : ReceiptStatus.PENDING)
                .stream().map(this::toView).toList();
    }

    /** Historial acumulado de recibos de un usuario (persona). */
    @Transactional(readOnly = true)
    public List<ReceiptView> byUser(UUID userId) {
        return receiptRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptView detail(UUID id) {
        return toView(require(id));
    }

    @Transactional
    public ReceiptView take(UUID receiptId, UUID staffUserId) {
        Receipt r = require(receiptId);
        r.setStatus(ReceiptStatus.IN_PROCESS);
        r.setAssignedStaffId(staffUserId);
        return toView(receiptRepository.save(r));
    }

    @Transactional
    public ReceiptView process(UUID receiptId, String note) {
        Receipt r = require(receiptId);
        r.setStatus(ReceiptStatus.PROCESSED);
        r.setProcessedAt(OffsetDateTime.now());
        if (note != null) r.setNotes(note);
        ReceiptView view = toView(receiptRepository.save(r));
        // Configurable desde el Backoffice: si el setting esta deshabilitado no se envia.
        eventNotificationService.sendEvent("RECEIPT_PROCESSED", r.getUserId(), "/home");
        return view;
    }

    @Transactional
    public ReceiptView observe(UUID receiptId, String note) {
        Receipt r = require(receiptId);
        r.setStatus(ReceiptStatus.OBSERVED);
        if (note != null) r.setNotes(note);
        ReceiptView view = toView(receiptRepository.save(r));
        // Configurable desde el Backoffice: si el setting esta deshabilitado no se envia.
        eventNotificationService.sendEvent("RECEIPT_OBSERVED", r.getUserId(), "/home");
        return view;
    }

    @Transactional
    public ReceiptView declareSire(UUID receiptId, String period) {
        Receipt r = require(receiptId);
        r.setDeclaredSire(true);
        r.setSirePeriod(period);
        r.setSireDeclaredAt(OffsetDateTime.now());
        return toView(receiptRepository.save(r));
    }

    @Transactional
    public ReceiptView declareSunat(UUID receiptId, String period) {
        Receipt r = require(receiptId);
        r.setDeclaredSunat(true);
        r.setSunatPeriod(period);
        r.setSunatDeclaredAt(OffsetDateTime.now());
        return toView(receiptRepository.save(r));
    }

    /** Datos fiscales del usuario (persona): RUC, regimen y Clave SOL (cifrada). */
    @Transactional
    public void upsertUserFiscal(UUID userId, String ruc, String regime, String solUser, String solPass) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        PersonProfile p = profileRepository.findByUserId(userId).orElseGet(() -> {
            PersonProfile n = new PersonProfile();
            n.setUserId(userId);
            return n;
        });
        if (ruc != null) p.setRuc(ruc);
        if (regime != null) p.setRegime(regime);
        if (solUser != null) p.setSolUser(solUser);
        if (solPass != null && !solPass.isBlank()) p.setSolPassEnc(crypto.encrypt(solPass));
        profileRepository.save(p);
    }

    /** Revela (descifra) la Clave SOL del usuario. Accion sensible y auditada. */
    @Transactional(readOnly = true)
    public RevealSolResponse revealSol(UUID userId, UUID staffId) {
        PersonProfile p = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no tiene datos fiscales."));
        if (p.getSolPassEnc() == null) {
            throw new ResourceNotFoundException("El usuario no tiene Clave SOL registrada.");
        }
        log.warn("REVEAL Clave SOL (persona): staff {} accedio a la Clave del usuario {}", staffId, userId);
        return new RevealSolResponse(p.getSolUser(), crypto.decrypt(p.getSolPassEnc()));
    }

    /** Asocia la ruta del archivo en Storage (lo hace la app movil al subir el comprobante). */
    @Transactional
    public ReceiptView attachFile(UUID receiptId, String filePath) {
        Receipt r = require(receiptId);
        r.setFilePath(filePath);
        return toView(receiptRepository.save(r));
    }

    /** Genera un signed URL de corta duracion para ver el archivo del recibo. */
    @Transactional(readOnly = true)
    public String fileUrl(UUID receiptId) {
        Receipt r = require(receiptId);
        if (r.getFilePath() == null || r.getFilePath().isBlank()) {
            throw new ResourceNotFoundException("Este recibo no tiene archivo adjunto.");
        }
        return fileStorage.signedReadUrl(r.getFilePath(), 10);
    }

    private Receipt require(UUID receiptId) {
        return receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResourceNotFoundException("Recibo no encontrado."));
    }

    private ReceiptView toView(Receipt r) {
        AppUser u = userRepository.findById(r.getUserId()).orElse(null);
        PersonProfile p = profileRepository.findByUserId(r.getUserId()).orElse(null);
        String userName = u != null ? (u.getDisplayName() != null ? u.getDisplayName() : u.getEmail()) : null;
        String userEmail = u != null ? u.getEmail() : null;
        return new ReceiptView(
                r.getId(), r.getUserId(), userName, userEmail,
                p != null ? p.getRuc() : null,
                p != null ? p.getRegime() : null,
                p != null && p.getSolPassEnc() != null,
                r.getType(), r.getDocNumber(), r.getIssueDate(), r.getAmount(), r.getCurrency(), r.getFileUrl(),
                r.getFilePath(), r.getFilePath() != null && !r.getFilePath().isBlank(),
                r.getIssuerRuc(), r.getStatus(), r.getAssignedStaffId(), r.getNotes(), r.getProcessedAt(),
                r.isDeclaredSire(), r.getSirePeriod(), r.getSireDeclaredAt(),
                r.isDeclaredSunat(), r.getSunatPeriod(), r.getSunatDeclaredAt());
    }
}
