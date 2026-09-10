package com.sumaup360.onboarding;

import com.sumaup360.auth.domain.AccountStatus;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.UserType;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.billing.service.SubscriptionService;
import com.sumaup360.common.error.ConflictException;
import com.sumaup360.common.error.ResourceNotFoundException;
import com.sumaup360.erp.module.service.ModuleEnablementService;
import com.sumaup360.tenant.domain.Company;
import com.sumaup360.tenant.domain.Tenant;
import com.sumaup360.tenant.repository.MembershipRepository;
import com.sumaup360.tenant.service.BranchService;
import com.sumaup360.tenant.service.CompanyService;
import com.sumaup360.tenant.service.MembershipService;
import com.sumaup360.tenant.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Onboarding self-service: un usuario recien registrado crea SU negocio sin necesitar staff.
 * Crea tenant + membresia (tenant-admin) + empresa + sucursales, asigna el plan FREE y
 * provisiona los modulos del rubro. El usuario pasa a ser BUSINESS.
 */
@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);
    private static final String FREE_PLAN = "erp-free";

    private final TenantService tenantService;
    private final MembershipService membershipService;
    private final MembershipRepository membershipRepository;
    private final CompanyService companyService;
    private final BranchService branchService;
    private final SubscriptionService subscriptionService;
    private final ModuleEnablementService moduleEnablementService;
    private final AppUserRepository userRepository;
    private final com.sumaup360.backoffice.service.ClientHistoryService clientHistory;

    public OnboardingService(TenantService tenantService,
                             MembershipService membershipService,
                             MembershipRepository membershipRepository,
                             CompanyService companyService,
                             BranchService branchService,
                             SubscriptionService subscriptionService,
                             ModuleEnablementService moduleEnablementService,
                             AppUserRepository userRepository,
                             com.sumaup360.backoffice.service.ClientHistoryService clientHistory) {
        this.tenantService = tenantService;
        this.membershipService = membershipService;
        this.membershipRepository = membershipRepository;
        this.companyService = companyService;
        this.branchService = branchService;
        this.subscriptionService = subscriptionService;
        this.moduleEnablementService = moduleEnablementService;
        this.userRepository = userRepository;
        this.clientHistory = clientHistory;
    }

    @Transactional(readOnly = true)
    public boolean isOnboarded(UUID userId) {
        return !membershipRepository.findByUserId(userId).isEmpty();
    }

    /**
     * Respuestas del wizard sobre como opera el negocio. Semantica: null = no tocar los
     * defaults del rubro; true/false = habilitar/deshabilitar el modulo como OVERRIDE.
     */
    public record OperationAnswers(
            Boolean sellsOnTables,        // tables + kitchen + menu
            Boolean tracksExpiry,         // batch-expiry
            Boolean takesAppointments,    // appointments
            Boolean takesCustomOrders,    // custom-orders
            Boolean handlesPrescriptions, // prescription
            Boolean sellsFood             // tables + kitchen + menu (hospedaje con restaurante)
    ) {
    }

    private static final Map<String, List<String>> ANSWER_MODULES = Map.of(
            "sellsOnTables", List.of("tables", "kitchen", "menu"),
            "tracksExpiry", List.of("batch-expiry"),
            "takesAppointments", List.of("appointments"),
            "takesCustomOrders", List.of("custom-orders"),
            "handlesPrescriptions", List.of("prescription"),
            "sellsFood", List.of("tables", "kitchen", "menu")
    );

    /**
     * Ejecuta el onboarding del usuario actual. Idempotencia: si ya tiene negocio, falla.
     */
    @Transactional
    public Result onboard(UUID userId, String firebaseUid, String businessName, String businessTypeCode,
                          String verticalCode, String ruc, List<String> branchNames,
                          OperationAnswers operation) {
        if (!membershipRepository.findByUserId(userId).isEmpty()) {
            throw new ConflictException("Ya tienes un negocio configurado.");
        }

        // 1) Tenant (incluye la creacion del rol tenant-admin)
        Tenant tenant = tenantService.create(slug(businessName), businessName);

        // 2) Membresia del usuario como administrador del negocio (asigna rol tenant-admin)
        membershipService.addMember(tenant.getId(), firebaseUid, true, true);

        // 3) El usuario pasa a ser de la Linea Negocios
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado."));
        user.setUserType(UserType.BUSINESS);
        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // 4) Empresa principal con su rubro
        Company company = companyService.create(tenant.getId(), businessName, emptyToNull(ruc),
                businessTypeCode, emptyToNull(verticalCode));

        // 5) Sucursales (al menos una)
        List<String> names = (branchNames == null || branchNames.isEmpty())
                ? List.of("Principal") : branchNames;
        boolean first = true;
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            branchService.create(tenant.getId(), company.getId(), name.trim(), null, first);
            first = false;
        }

        // 6) Plan FREE
        subscriptionService.subscribeTenant(tenant.getId(), FREE_PLAN);

        // 7) Provisiona modulos del rubro y aplica las respuestas del wizard como overrides
        moduleEnablementService.provision(tenant.getId(), company.getId());
        applyOperationAnswers(tenant.getId(), company.getId(), operation);

        // 8) Historial: alta del cliente
        clientHistory.record(tenant.getId(), "CLIENT_CREATED",
                "Negocio creado (" + businessName + ") con plan Free", userId);

        log.info("Onboarding completo: tenant {} ({}) para uid {}", tenant.getId(), businessName, firebaseUid);
        return new Result(tenant.getId(), company.getId());
    }

    public record Result(UUID tenantId, UUID companyId) {
    }

    /**
     * Traduce las respuestas del wizard a overrides de modulos. Solo toca un modulo cuando
     * la respuesta difiere del default del rubro (asi el estado queda como DEFAULT si coincide).
     */
    private void applyOperationAnswers(UUID tenantId, UUID companyId, OperationAnswers operation) {
        if (operation == null) {
            return;
        }
        Set<String> enabled = new HashSet<>(moduleEnablementService.enabledCodes(tenantId, companyId));
        Map<String, Boolean> answers = new LinkedHashMap<>();
        answers.put("sellsOnTables", operation.sellsOnTables());
        answers.put("tracksExpiry", operation.tracksExpiry());
        answers.put("takesAppointments", operation.takesAppointments());
        answers.put("takesCustomOrders", operation.takesCustomOrders());
        answers.put("handlesPrescriptions", operation.handlesPrescriptions());
        answers.put("sellsFood", operation.sellsFood());
        answers.forEach((key, value) -> {
            if (value == null) return;
            for (String moduleCode : ANSWER_MODULES.get(key)) {
                if (enabled.contains(moduleCode) != value) {
                    moduleEnablementService.setModule(tenantId, companyId, moduleCode, value);
                }
            }
        });
    }

    private String slug(String name) {
        String base = name == null ? "negocio" : name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "negocio";
        if (base.length() > 30) base = base.substring(0, 30);
        return base + "-" + UUID.randomUUID().toString().substring(0, 5);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
