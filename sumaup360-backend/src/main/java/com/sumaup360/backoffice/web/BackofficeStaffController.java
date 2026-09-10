package com.sumaup360.backoffice.web;

import com.sumaup360.backoffice.dto.StaffDtos.CreateStaffRequest;
import com.sumaup360.backoffice.dto.StaffDtos.StaffView;
import com.sumaup360.backoffice.service.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Backoffice: gestion de personal interno (staff). Sin auto-registro. */
@RestController
@RequestMapping("/api/v1/backoffice/staff")
@Tag(name = "Backoffice - Staff", description = "Creacion y listado de personal interno")
public class BackofficeStaffController {

    private final StaffService staffService;

    public BackofficeStaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('staff:read')")
    @Operation(summary = "Lista el personal interno (requiere staff:read)")
    public List<StaffView> list() {
        return staffService.list().stream().map(StaffView::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('staff:manage')")
    @Operation(summary = "Crea una cuenta de staff (correo+contrasena en Firebase) (requiere staff:manage)")
    public StaffView create(@Valid @RequestBody CreateStaffRequest req) {
        return StaffView.from(staffService.create(req.email(), req.password(), req.name(), req.roleCode()));
    }
}
