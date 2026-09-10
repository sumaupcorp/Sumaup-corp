package com.sumaup360.app.service;

import com.sumaup360.app.domain.Alert;
import com.sumaup360.app.dto.AlertDtos.CreateAlertRequest;
import com.sumaup360.app.enums.AlertStatus;
import com.sumaup360.app.enums.AlertType;
import com.sumaup360.app.repository.AlertRepository;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Alertas/recordatorios de la persona. */
@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Transactional
    public Alert create(UUID userId, CreateAlertRequest req) {
        Alert a = new Alert();
        a.setUserId(userId);
        a.setType(req.type() != null ? req.type() : AlertType.INFO);
        a.setTitle(req.title());
        a.setMessage(req.message());
        a.setDueDate(req.dueDate());
        a.setStatus(AlertStatus.PENDING);
        return alertRepository.save(a);
    }

    @Transactional(readOnly = true)
    public List<Alert> list(UUID userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Alert updateStatus(UUID id, UUID userId, AlertStatus status) {
        Alert a = alertRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Alerta no encontrada."));
        a.setStatus(status);
        return alertRepository.save(a);
    }
}
