package com.sumaup360.notification.web;

import com.sumaup360.notification.dto.CampaignDtos.CampaignView;
import com.sumaup360.notification.dto.CampaignDtos.CreateCampaignRequest;
import com.sumaup360.notification.dto.CampaignDtos.SettingView;
import com.sumaup360.notification.dto.CampaignDtos.UpdateSettingRequest;
import com.sumaup360.notification.service.CampaignService;
import com.sumaup360.notification.service.NotificationSettingService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Backoffice (staff): campanas de push (publicidad/recordatorios) programables y
 * configuracion de las notificaciones automaticas de la Linea Personas.
 */
@RestController
@RequestMapping("/api/v1/backoffice/notifications")
@Tag(name = "Backoffice - Campanas de push",
        description = "Campanas de push programables y configuracion de notificaciones automaticas")
public class NotificationCampaignAdminController {

    private final CampaignService campaignService;
    private final NotificationSettingService settingService;

    public NotificationCampaignAdminController(CampaignService campaignService,
                                               NotificationSettingService settingService) {
        this.campaignService = campaignService;
        this.settingService = settingService;
    }

    // --- Campanas ---

    @GetMapping("/campaigns")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Lista las campanas de push (requiere person:profile:manage)")
    public List<CampaignView> campaigns() {
        return campaignService.list().stream().map(CampaignView::from).toList();
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Crea una campana de push: inmediata o programada (requiere person:profile:manage)")
    public CampaignView create(@Valid @RequestBody CreateCampaignRequest req) {
        return CampaignView.from(campaignService.create(
                req.title(), req.body(), req.route(), req.audience(), req.scheduledAt(),
                SecurityUtils.currentPrincipal().userId()));
    }

    @PostMapping("/campaigns/{id}/cancel")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Cancela una campana programada (requiere person:profile:manage)")
    public CampaignView cancel(@PathVariable UUID id) {
        return CampaignView.from(campaignService.cancel(id));
    }

    // --- Configuracion de notificaciones automaticas ---

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Configuracion de notificaciones automaticas (requiere person:profile:manage)")
    public List<SettingView> settings() {
        return settingService.list().stream().map(SettingView::from).toList();
    }

    @PutMapping("/settings/{key}")
    @PreAuthorize("hasAuthority('person:profile:manage')")
    @Operation(summary = "Activa/desactiva y edita los textos de una notificacion automatica "
            + "(requiere person:profile:manage)")
    public SettingView updateSetting(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest req) {
        return SettingView.from(settingService.update(key, req.enabled(), req.title(), req.body()));
    }
}
