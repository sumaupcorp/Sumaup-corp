package com.sumaup360.backoffice.web;

import com.sumaup360.backoffice.dto.ClientsDtos.ClientView;
import com.sumaup360.backoffice.dto.ClientsDtos.HistoryEvent;
import com.sumaup360.backoffice.dto.FiscalDtos.CredentialsView;
import com.sumaup360.backoffice.dto.FiscalDtos.RevealResponse;
import com.sumaup360.backoffice.dto.FiscalDtos.UpsertCredentialsRequest;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.backoffice.service.ClientHistoryService;
import com.sumaup360.backoffice.service.ClientsService;
import com.sumaup360.backoffice.service.FiscalCredentialsService;
import com.sumaup360.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Backoffice: panorama de clientes y boveda de credenciales fiscales. Cross-tenant (staff). */
@RestController
@RequestMapping("/api/v1/backoffice/clients")
@Tag(name = "Backoffice - Clientes", description = "Clientes del SaaS y sus credenciales fiscales")
public class BackofficeClientsController {

    private final ClientsService clientsService;
    private final FiscalCredentialsService credentialsService;
    private final ClientHistoryService historyService;
    private final AppUserRepository userRepository;

    public BackofficeClientsController(ClientsService clientsService,
                                       FiscalCredentialsService credentialsService,
                                       ClientHistoryService historyService,
                                       AppUserRepository userRepository) {
        this.clientsService = clientsService;
        this.credentialsService = credentialsService;
        this.historyService = historyService;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    @Operation(summary = "Lista todos los clientes (tenants) del SaaS (requiere client:read)")
    public List<ClientView> list() {
        return clientsService.list();
    }

    @GetMapping("/{tenantId}/history")
    @PreAuthorize("hasAuthority('client:read')")
    @Operation(summary = "Historial de eventos del cliente (requiere client:read)")
    public List<HistoryEvent> history(@PathVariable UUID tenantId) {
        var events = historyService.list(tenantId);
        var actorIds = events.stream().map(e -> e.getActorUserId()).filter(java.util.Objects::nonNull).distinct().toList();
        var names = userRepository.findAllById(actorIds).stream()
                .collect(java.util.stream.Collectors.toMap(AppUser::getId,
                        u -> u.getDisplayName() != null ? u.getDisplayName() : (u.getEmail() != null ? u.getEmail() : "—")));
        return events.stream()
                .map(e -> HistoryEvent.from(e, e.getActorUserId() != null ? names.getOrDefault(e.getActorUserId(), "—") : "Sistema"))
                .toList();
    }

    @GetMapping("/{tenantId}/credentials")
    @PreAuthorize("hasAuthority('sol:read')")
    @Operation(summary = "Credenciales fiscales (enmascaradas) de un cliente (requiere sol:read)")
    public CredentialsView credentials(@PathVariable UUID tenantId) {
        return credentialsService.get(tenantId);
    }

    @PutMapping("/{tenantId}/credentials")
    @PreAuthorize("hasAuthority('sol:manage')")
    @Operation(summary = "Registra/actualiza credenciales fiscales (requiere sol:manage)")
    public CredentialsView upsert(@PathVariable UUID tenantId,
                                  @Valid @RequestBody UpsertCredentialsRequest req) {
        return credentialsService.upsert(tenantId, req, SecurityUtils.currentPrincipal().userId());
    }

    @PostMapping("/{tenantId}/credentials/reveal")
    @PreAuthorize("hasAuthority('sol:reveal')")
    @Operation(summary = "Revela (descifra) la Clave SOL — accion auditada (requiere sol:reveal)")
    public RevealResponse reveal(@PathVariable UUID tenantId) {
        return credentialsService.reveal(tenantId, SecurityUtils.currentPrincipal().userId());
    }
}
