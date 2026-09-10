package com.sumaup360.app.feature;

import com.sumaup360.app.domain.PersonProfile;
import com.sumaup360.app.enums.ClientType;
import com.sumaup360.app.repository.PersonProfileRepository;
import com.sumaup360.billing.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio central de permisos por feature/plan/tipo de cliente. La app/backoffice deben
 * consultar aqui ANTES de permitir una accion premium. Server-side: no confiar en el cliente.
 *
 * FASE 1: el estado premium aun no se activa (activacion manual desde backoffice llega en
 * FASE 2). Por ahora isPremium=false; la estructura de bloqueo ya es la definitiva.
 */
@Service
public class FeatureAccessService {

    /** Features disponibles en plan FREE (con limites de uso aparte para IA/SUNAT). */
    private static final Set<Feature> FREE_FEATURES = EnumSet.of(
            Feature.CHAT_IA, Feature.CONSULTA_SUNAT);

    private static final BigDecimal PRICE_TAXISTA = new BigDecimal("39.90");
    private static final BigDecimal PRICE_PEYA = new BigDecimal("29.90");
    private static final BigDecimal PRICE_SERVICIOS = new BigDecimal("14.90");

    private final PersonProfileRepository profileRepository;
    private final SubscriptionService subscriptionService;

    public FeatureAccessService(PersonProfileRepository profileRepository, SubscriptionService subscriptionService) {
        this.profileRepository = profileRepository;
        this.subscriptionService = subscriptionService;
    }

    /** Resultado del chequeo de acceso. */
    public record Access(boolean allowed, String reason, String planRequired, BigDecimal price) {
        static Access ok() {
            return new Access(true, "ALLOWED", null, null);
        }
    }

    @Transactional(readOnly = true)
    public Access check(UUID userId, Feature feature) {
        PersonProfile p = profileRepository.findByUserId(userId).orElse(null);
        ClientType ct = p != null ? p.getClientType() : null;

        if (FREE_FEATURES.contains(feature)) {
            return Access.ok();
        }
        if (isPremium(userId)) {
            return Access.ok();
        }
        // Bloqueada: indicar el plan requerido segun el tipo de cliente.
        String planRequired;
        BigDecimal price;
        if (ct == ClientType.DELIVERY_PEYA) {
            planRequired = "PEYA_PREMIUM";
            price = PRICE_PEYA;
        } else if (ct == ClientType.SERVICIOS_PROFESIONALES) {
            planRequired = "SERV_PREMIUM";
            price = PRICE_SERVICIOS;
        } else {
            planRequired = "TAXISTA_PREMIUM";
            price = PRICE_TAXISTA;
        }
        return new Access(false, "REQUIRES_PREMIUM", planRequired, price);
    }

    /** Premium = suscripcion activa cuyo plan contiene "premium" (activada manual en backoffice). */
    private boolean isPremium(UUID userId) {
        return subscriptionService.isUserPremium(userId);
    }
}
