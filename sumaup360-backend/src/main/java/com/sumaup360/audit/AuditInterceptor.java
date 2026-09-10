package com.sumaup360.audit;

import com.sumaup360.audit.service.AuditService;
import com.sumaup360.security.AppUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/** Audita automaticamente toda mutacion (POST/PUT/PATCH/DELETE) sobre /api/v1. */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuditInterceptor.class);
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditService auditService;

    public AuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            if (!MUTATIONS.contains(request.getMethod())) {
                return;
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal p) {
                auditService.record(p.userId(), p.userType() != null ? p.userType().name() : null,
                        request.getMethod(), request.getRequestURI(), response.getStatus(),
                        p.tenantId(), request.getRemoteAddr());
            } else {
                auditService.record(null, "ANONYMOUS", request.getMethod(),
                        request.getRequestURI(), response.getStatus(), null, request.getRemoteAddr());
            }
        } catch (Exception e) {
            // La auditoria nunca debe romper la respuesta.
            log.warn("No se pudo auditar la peticion: {}", e.getMessage());
        }
    }
}
