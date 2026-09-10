package com.sumaup360.notification.web;

import com.sumaup360.notification.dto.NotificationDtos.NotificationView;
import com.sumaup360.notification.dto.NotificationDtos.SendNotificationRequest;
import com.sumaup360.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Envio de notificaciones por staff/sistema. */
@RestController
@RequestMapping("/api/v1/backoffice/notifications")
@Tag(name = "Backoffice - Notificaciones", description = "Envio de notificaciones a usuarios")
public class NotificationAdminController {

    private final NotificationService notificationService;

    public NotificationAdminController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('notification:send')")
    @Operation(summary = "Envia una notificacion a un usuario (requiere notification:send)")
    public NotificationView send(@Valid @RequestBody SendNotificationRequest req) {
        return NotificationView.from(notificationService.create(
                req.recipientUserId(), req.title(), req.body(), req.type()));
    }
}
