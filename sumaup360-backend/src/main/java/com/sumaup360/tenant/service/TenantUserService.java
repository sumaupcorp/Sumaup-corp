package com.sumaup360.tenant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sumaup360.auth.domain.AccountStatus;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.Role;
import com.sumaup360.auth.domain.UserType;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.auth.repository.RoleRepository;
import com.sumaup360.auth.service.FirebaseAccountService;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.tenant.domain.Branch;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.domain.Membership;
import com.sumaup360.tenant.repository.BranchRepository;
import com.sumaup360.tenant.repository.CompanyRepository;
import com.sumaup360.tenant.repository.MembershipRepository;
import com.sumaup360.tenant.web.dto.TenantUserDtos.CreateWorkerRequest;
import com.sumaup360.tenant.web.dto.TenantUserDtos.TenantUserResponse;
import com.sumaup360.tenant.web.dto.TenantUserDtos.UpdateAssignmentRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion de usuarios (trabajadores) dentro de un tenant: alta de cuentas, roles del
 * tenant, sede asignada y modulos visibles. Cierra la jerarquia RBAC a nivel de Empresa.
 */
@Service
public class TenantUserService {

    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final CompanyRepository companyRepository;
    private final FirebaseAccountService firebaseAccountService;
    private final ObjectMapper objectMapper;

    public TenantUserService(MembershipRepository membershipRepository,
                             AppUserRepository userRepository,
                             RoleRepository roleRepository,
                             BranchRepository branchRepository,
                             CompanyRepository companyRepository,
                             FirebaseAccountService firebaseAccountService,
                             ObjectMapper objectMapper) {
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.branchRepository = branchRepository;
        this.companyRepository = companyRepository;
        this.firebaseAccountService = firebaseAccountService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TenantUserResponse> listTenantUsers(UUID tenantId) {
        List<Membership> memberships = membershipRepository.findByTenantId(tenantId);
        Map<UUID, Branch> branches = branchRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Function.identity()));
        return memberships.stream()
                .map(m -> {
                    AppUser u = userRepository.findWithRolesById(m.getUserId()).orElse(null);
                    return u != null ? toResponse(u, m, branches.get(m.getBranchId())) : null;
                })
                .filter(r -> r != null)
                .toList();
    }

    /**
     * Alta de un trabajador: crea la cuenta Firebase (correo+contrasena), el AppUser
     * BUSINESS y la membresia del tenant con rol, sede y modulos opcionales.
     */
    @Transactional
    public TenantUserResponse createWorker(UUID tenantId, CreateWorkerRequest req) {
        Role role = null;
        if (req.roleId() != null) {
            role = roleRepository.findByIdAndTenantId(req.roleId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en este tenant."));
        }
        Branch branch = requireBranchOrNull(tenantId, req.branchId());

        String uid = firebaseAccountService.createUser(req.email().trim(), req.password());

        AppUser user = new AppUser();
        user.setFirebaseUid(uid);
        user.setEmail(req.email().trim());
        user.setDisplayName(req.name().trim());
        user.setUserType(UserType.BUSINESS);
        user.setStatus(AccountStatus.ACTIVE);
        if (role != null) {
            user.getRoles().add(role);
        }
        user = userRepository.save(user);

        Membership m = new Membership();
        m.setUserId(user.getId());
        m.setTenantId(tenantId);
        m.setDefaultTenant(true);
        m.setStatus("ACTIVE");
        m.setBranchId(branch != null ? branch.getId() : null);
        m.setAllowedModules(toJson(req.allowedModules()));
        membershipRepository.save(m);

        return toResponse(user, m, branch);
    }

    /** Cambia la sede asignada y los modulos visibles (PUT completo de la asignacion). */
    @Transactional
    public TenantUserResponse updateAssignment(UUID tenantId, UUID userId, UpdateAssignmentRequest req) {
        Membership m = membershipRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("El usuario no pertenece a este tenant."));
        Branch branch = requireBranchOrNull(tenantId, req.branchId());
        m.setBranchId(branch != null ? branch.getId() : null);
        m.setAllowedModules(toJson(req.allowedModules()));
        membershipRepository.save(m);
        AppUser user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        return toResponse(user, m, branch);
    }

    @Transactional
    public TenantUserResponse assignRole(UUID tenantId, UUID userId, UUID roleId) {
        AppUser user = requireMember(tenantId, userId);
        Role role = roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en este tenant."));
        user.getRoles().add(role);
        userRepository.save(user);
        return withMembership(user, tenantId);
    }

    @Transactional
    public TenantUserResponse revokeRole(UUID tenantId, UUID userId, UUID roleId) {
        AppUser user = requireMember(tenantId, userId);
        // Solo se pueden quitar roles de ESTE tenant (aislamiento).
        roleRepository.findByIdAndTenantId(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado en este tenant."));
        user.getRoles().removeIf(r -> r.getId().equals(roleId));
        userRepository.save(user);
        return withMembership(user, tenantId);
    }

    private Branch requireBranchOrNull(UUID tenantId, UUID branchId) {
        if (branchId == null) {
            return null;
        }
        return branchRepository.findByIdAndTenantId(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Sucursal no encontrada en este tenant."));
    }

    private AppUser requireMember(UUID tenantId, UUID userId) {
        if (!membershipRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new ResourceNotFoundException("El usuario no pertenece a este tenant.");
        }
        return userRepository.findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
    }

    private TenantUserResponse withMembership(AppUser user, UUID tenantId) {
        Membership m = membershipRepository.findByUserIdAndTenantId(user.getId(), tenantId).orElse(null);
        Branch branch = m != null && m.getBranchId() != null
                ? branchRepository.findById(m.getBranchId()).orElse(null) : null;
        return toResponse(user, m, branch);
    }

    /** Negocio (empresa) al que pertenece la sede asignada; null si no tiene sede. */
    private Company companyOf(Branch branch) {
        return branch != null ? companyRepository.findById(branch.getCompanyId()).orElse(null) : null;
    }

    /** Mapea el usuario mostrando solo sus roles de este tenant + su asignacion operativa. */
    private TenantUserResponse toResponse(AppUser u, Membership m, Branch branch) {
        UUID tenantId = m != null ? m.getTenantId() : null;
        List<String> roles = u.getRoles().stream()
                .filter(r -> tenantId != null && tenantId.equals(r.getTenantId()))
                .map(Role::getCode)
                .toList();
        Company company = companyOf(branch);
        return new TenantUserResponse(u.getId(), u.getFirebaseUid(), u.getEmail(),
                u.getDisplayName(), u.getUserType(), roles,
                m != null ? m.getBranchId() : null,
                branch != null ? branch.getName() : null,
                company != null ? company.getId() : null,
                company != null ? company.getLegalName() : null,
                parseModules(m != null ? m.getAllowedModules() : null));
    }

    private String toJson(List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return null; // null = todos los modulos habilitados
        }
        try {
            return objectMapper.writeValueAsString(modules);
        } catch (Exception e) {
            throw new BadRequestException("Lista de modulos invalida.");
        }
    }

    private List<String> parseModules(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception e) {
            return null;
        }
    }
}
