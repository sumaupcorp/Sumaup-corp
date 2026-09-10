package com.sumaup360.notification.dto;

import com.sumaup360.notification.domain.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record SendNotificationRequest(
            @NotNull UUID recipientUserId,
            @NotBlank String title,
            String body,
            String type
    ) {
    }

    public record NotificationView(UUID id, String title, String body, String type,
                                   boolean read, OffsetDateTime createdAt) {
        public static NotificationView from(Notification n) {
            return new NotificationView(n.getId(), n.getTitle(), n.getBody(), n.getType(),
                    n.isRead(), n.getCreatedAt());
        }
    }
}
