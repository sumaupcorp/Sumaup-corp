package com.sumaup360.app.honorarios.service;

import com.sumaup360.app.feature.Feature;
import com.sumaup360.app.feature.FeatureAccessService;
import com.sumaup360.app.honorarios.domain.HonorarioRequest;
import com.sumaup360.app.honorarios.dto.HonorariosDtos.CreateHonorarioRequest;
import com.sumaup360.app.honorarios.repository.HonorarioRequestRepository;
import com.sumaup360.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Acciones del profesional (Premium): solicitar la generacion de un Recibo por Honorarios. */
@Service
public class HonorarioService {

    private final HonorarioRequestRepository repository;
    private final FeatureAccessService featureAccess;

    public HonorarioService(HonorarioRequestRepository repository, FeatureAccessService featureAccess) {
        this.repository = repository;
        this.featureAccess = featureAccess;
    }

    @Transactional
    public HonorarioRequest create(UUID userId, CreateHonorarioRequest req) {
        requirePremium(userId);
        HonorarioRequest h = new HonorarioRequest();
        h.setUserId(userId);
        h.setClienteNombre(req.clienteNombre().trim());
        h.setClienteDocType(req.clienteDocType());
        h.setClienteDocNumber(req.clienteDocNumber());
        h.setDescripcion(req.descripcion().trim());
        h.setMonto(req.monto());
        h.setConRetencion(Boolean.TRUE.equals(req.conRetencion()));
        return repository.save(h);
    }

    @Transactional(readOnly = true)
    public List<HonorarioRequest> myRequests(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private void requirePremium(UUID userId) {
        FeatureAccessService.Access a = featureAccess.check(userId, Feature.HONORARIOS_RECIBO);
        if (!a.allowed()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED,
                    "Funcion Premium. Activa tu plan para solicitar tus recibos por honorarios.");
        }
    }
}
