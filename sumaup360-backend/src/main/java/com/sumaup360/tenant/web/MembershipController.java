package com.sumaup360.tenant.web;

import com.sumaup360.tenant.service.MembershipService;
import com.sumaup360.tenant.web.dto.MembershipDtos.CreateMembershipRequest;
import com.sumaup360.tenant.web.dto.MembershipDtos.MembershipResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Asigna usuarios a un tenant (requiere tenant:manage). */
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/members")
@Tag(name = "Membresias", description = "Vinculo usuario <-> tenant")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tenant:manage')")
    @Operation(summary = "Asocia un usuario (por firebaseUid) al tenant")
    public MembershipResponse add(@PathVariable UUID tenantId,
                                  @Valid @RequestBody CreateMembershipRequest req) {
        boolean asDefault = req.asDefault() == null || req.asDefault();
        boolean asAdmin = req.asAdmin() != null && req.asAdmin();
        return MembershipResponse.from(
                membershipService.addMember(tenantId, req.firebaseUid(), asDefault, asAdmin));
    }
}
