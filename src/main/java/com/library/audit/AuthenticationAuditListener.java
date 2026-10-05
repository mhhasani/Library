package com.library.audit;

import com.library.entity.User;
import com.library.entity.enums.AuditAction;
import com.library.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Records every login attempt, successful or not. Spring Security publishes these events
 * for every authentication that goes through the AuthenticationManager.
 */
@Component
public class AuthenticationAuditListener {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AuthenticationAuditListener(AuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String name = event.getAuthentication().getName();
        auditService.record(AuditEntry.success(AuditAction.LOGIN)
                .entityType("USER")
                .actorId(userIdFor(name)).actorEmail(name)
                .build());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String name = event.getAuthentication().getName();
        // The reason is recorded for auditors only; the user always sees one generic message
        auditService.record(AuditEntry.failure(AuditAction.LOGIN)
                .entityType("USER")
                .actorId(userIdFor(name)).actorEmail(name)
                .details(event.getException().getClass().getSimpleName())
                .build());
    }

    private Long userIdFor(String email) {
        return email == null ? null : userRepository.findByEmail(email).map(User::getId).orElse(null);
    }
}
