package com.sumaup360.notification.dto;

import com.sumaup360.notification.domain.Campaign;
import com.sumaup360.notification.domain.NotificationSetting;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/** DTOs de campanas de push y configuracion de notificaciones automaticas (Backoffice). */
public final class CampaignDtos {

    private CampaignDtos() {
    }

    public record CreateCampaignRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 500) String body,
            @Size(max = 120) String route,
            @NotBlank String audience,
            OffsetDateTime scheduledAt
    ) {
    }

    public record CampaignView(UUID id, String title, String body, String route,
                               String audience, OffsetDateTime scheduledAt, String status,
                               int sentCount, int failedCount,
                               OffsetDateTime createdAt, OffsetDateTime sentAt) {
        public static CampaignView from(Campaign c) {
            return new CampaignView(c.getId(), c.getTitle(), c.getBody(), c.getRoute(),
                    c.getAudience().name(), c.getScheduledAt(), c.getStatus().name(),
                    c.getSentCount(), c.getFailedCount(), c.getCreatedAt(), c.getSentAt());
        }
    }

    public record UpdateSettingRequest(
            @NotNull Boolean enabled,
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 500) String body
    ) {
    }

    public record SettingView(String key, boolean enabled, String title, String body,
                              String description, OffsetDateTime updatedAt) {
        public static SettingView from(NotificationSetting s) {
            return new SettingView(s.getKey(), s.isEnabled(), s.getTitle(), s.getBody(),
                    s.getDescription(), s.getUpdatedAt());
        }
    }
}
