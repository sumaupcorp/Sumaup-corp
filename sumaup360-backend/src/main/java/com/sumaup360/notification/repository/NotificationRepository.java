package com.sumaup360.notification.repository;

import com.sumaup360.notification.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);
    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);
}
