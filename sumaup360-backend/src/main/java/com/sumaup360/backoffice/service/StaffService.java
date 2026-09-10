package com.sumaup360.backoffice.service;

import com.sumaup360.auth.domain.AccountStatus;
import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.Role;
import com.sumaup360.auth.domain.UserType;
import com.sumaup360.auth.repository.AppUserRepository;
import com.sumaup360.auth.repository.RoleRepository;
import com.sumaup360.auth.service.FirebaseAccountService;
import com.sumaup360.common.error.BadRequestException;
import com.sumaup360.common.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de personal interno (staff). Las cuentas se crean DESDE el Backoffice (no hay
 * auto-registro): se crea el usuario en Firebase (correo+contrasena, sin Google) y se le
 * asigna su rol de staff. Control total del superadmin. El alta en Firebase la hace
 * FirebaseAccountService (REST Identity Toolkit, compartido con el SaaS).
 */
@Service
public class StaffService {

    private final AppUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FirebaseAccountService firebaseAccountService;

    public StaffService(AppUserRepository userRepository,
                        RoleRepository roleRepository,
                        FirebaseAccountService firebaseAccountService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.firebaseAccountService = firebaseAccountService;
    }

    @Transactional(readOnly = true)
    public List<AppUser> list() {
        return userRepository.findByUserType(UserType.STAFF);
    }

    @Transactional
    public AppUser create(String email, String password, String name, String roleCode) {
        Role role = roleRepository.findByCodeAndTenantIdIsNull(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rol de staff no encontrado: " + roleCode));

        String uid = firebaseAccountService.createUser(email, password);

        AppUser user = new AppUser();
        user.setFirebaseUid(uid);
        user.setEmail(email);
        user.setDisplayName(name);
        user.setUserType(UserType.STAFF);
        user.setStatus(AccountStatus.ACTIVE);
        user.getRoles().add(role);
        return userRepository.save(user);
    }
}
