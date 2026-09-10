package com.sumaup360.app.honorarios.service;

import com.sumaup360.app.feature.Feature;
import com.sumaup360.app.feature.FeatureAccessService;
import com.sumaup360.app.honorarios.domain.SuspensionRequest;
import com.sumaup360.app.honorarios.repository.SuspensionRequestRepository;
import com.sumaup360.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Acciones del profesional (Premium): solicitar la Suspension de 4ta categoria (anual). */
@Service
public class SuspensionService {

    private final SuspensionRequestRepository repository;
    private final FeatureAccessService featureAccess;

    public SuspensionService(SuspensionRequestRepository repository, FeatureAccessService featureAccess) {
        this.repository = repository;
        this.featureAccess = featureAccess;
    }

    @Transactional
    public SuspensionRequest create(UUID userId, Integer anio) {
        requirePremium(userId);
        if (anio == null || anio < 2000 || anio > 2100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa un año valido.");
        }
        if (repository.existsByUserIdAndAnio(userId, anio)) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya tienes una solicitud de suspension para ese año.");
        }
        SuspensionRequest s = new SuspensionRequest();
        s.setUserId(userId);
        s.setAnio(anio);
        return repository.save(s);
    }

    @Transactional(readOnly = true)
    public List<SuspensionRequest> myRequests(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void requirePremium(UUID userId) {
        FeatureAccessService.Access a = featureAccess.check(userId, Feature.HONORARIOS_SUSPENSION);
        if (!a.allowed()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Funcion Premium. Activa tu plan para tramitar tu suspension de 4ta.");
        }
    }
}
