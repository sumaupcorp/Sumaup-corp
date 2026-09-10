package com.sumaup360.notification.service;

import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.notification.domain.Notification;
import com.sumaup360.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Notificaciones in-app. Otros modulos pueden inyectar este servicio para notificar. */
@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Notification create(UUID recipientUserId, String title, String body, String type) {
        Notification n = new Notification();
        n.setRecipientUserId(recipientUserId);
        n.setTitle(title);
        n.setBody(body);
        if (type != null && !type.isBlank()) {
            n.setType(type);
        }
        n.setRead(false);
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public List<Notification> listForUser(UUID userId) {
        return repository.findByRecipientUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Notification markRead(UUID id, UUID userId) {
        Notification n = repository.findByIdAndRecipientUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion no encontrada."));
        n.setRead(true);
        return repository.save(n);
    }
}
