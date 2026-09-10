package com.sumaup360.auth.repository;

import com.sumaup360.auth.domain.AppUser;
import com.sumaup360.auth.domain.UserType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByFirebaseUid(String firebaseUid);

    /** Usuarios por tipo (p. ej. STAFF para el Backoffice), con roles cargados. */
    @EntityGraph(attributePaths = "roles")
    List<AppUser> findByUserType(UserType userType);

    /** Carga el usuario con sus roles y permisos en una sola consulta (para /me y autorizacion). */
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<AppUser> findWithRolesByFirebaseUid(String firebaseUid);

    /** Carga el usuario con sus roles por id (para gestion de usuarios del tenant). */
    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findWithRolesById(UUID id);
}
