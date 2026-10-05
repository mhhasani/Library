package com.library.audit;

import com.library.entity.enums.AuditAction;
import com.library.entity.enums.AuditOutcome;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * Trail of every data-changing API call (POST/PUT/PATCH/DELETE): who, what endpoint, and
 * whether it succeeded. Read-only requests are not recorded. Request bodies are never
 * logged, so no sensitive input ends up in the audit trail.
 */
@Component
public class ApiWriteAuditInterceptor implements HandlerInterceptor {

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuditService auditService;

    public ApiWriteAuditInterceptor(AuditService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!WRITE_METHODS.contains(request.getMethod())) {
            return;
        }
        int status = response.getStatus();
        boolean ok = ex == null && status < 400;
        auditService.record(AuditEntry.builder()
                .action(AuditAction.API_WRITE)
                .outcome(ok ? AuditOutcome.SUCCESS : AuditOutcome.FAILURE)
                .entityType("API")
                .details(request.getMethod() + " " + request.getRequestURI() + " -> " + status)
                .build());
    }
}
