package com.sumaup360.notification.web;

import com.sumaup360.notification.dto.NotificationDtos.NotificationView;
import com.sumaup360.notification.service.NotificationService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Notificaciones del usuario autenticado (cualquier linea). */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notificaciones", description = "Notificaciones in-app del usuario")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "Lista mis notificaciones")
    public List<NotificationView> mine() {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return notificationService.listForUser(userId).stream().map(NotificationView::from).toList();
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marca una notificacion como leida")
    public NotificationView markRead(@PathVariable UUID id) {
        UUID userId = SecurityUtils.currentPrincipal().userId();
        return NotificationView.from(notificationService.markRead(id, userId));
    }
}
