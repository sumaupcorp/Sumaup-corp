package com.sumaup360.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.sumaup360.auth.service.IdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Verifica la identidad en cada request y arma el SecurityContext.
 *
 * 1) Si Firebase esta configurado y llega "Authorization: Bearer <idToken>", verifica el token.
 * 2) Si no, y security.dev-mode=true, acepta el header X-Debug-Uid para pruebas locales.
 * 3) Si no hay identidad, la request sigue anonima (los endpoints protegidos responderan 401).
 *
 * La autorizacion real (permisos) la decide el backend via IdentityService.
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FirebaseTokenFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final IdentityService identityService;
    private final SecurityProperties securityProperties;

    public FirebaseTokenFilter(ObjectProvider<FirebaseAuth> firebaseAuthProvider,
                               IdentityService identityService,
                               SecurityProperties securityProperties) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.identityService = identityService;
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                authenticate(request);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String uid = null;
        String email = null;
        String name = null;

        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        String header = request.getHeader("Authorization");

        if (firebaseAuth != null && header != null && header.startsWith(BEARER_PREFIX)) {
            String idToken = header.substring(BEARER_PREFIX.length()).trim();
            try {
                FirebaseToken token = firebaseAuth.verifyIdToken(idToken);
                uid = token.getUid();
                email = token.getEmail();
                name = token.getName();
            } catch (Exception e) {
                log.debug("idToken invalido: {}", e.getMessage());
                return; // request queda anonima
            }
        } else if (securityProperties.isDevMode()) {
            String debugUid = request.getHeader("X-Debug-Uid");
            if (debugUid != null && !debugUid.isBlank()) {
                uid = debugUid.trim();
                email = request.getHeader("X-Debug-Email");
                name = request.getHeader("X-Debug-Name");
                log.warn("DEV-MODE: autenticando por X-Debug-Uid={} (NO usar en produccion).", uid);
            }
        }

        if (uid == null) {
            return;
        }

        AppUserPrincipal principal = identityService.authenticate(uid, email, name);

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        principal.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        principal.roles().forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        TenantContext.set(principal.tenantId());
    }
}
