package com.library.keycloak;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.entity.User;
import com.library.entity.enums.AuditAction;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.UserRepository;
import com.library.util.SecurityUtils;
import org.springframework.stereotype.Service;

/**
 * Administrator actions on a user's identity-provider account. Passwords are only passed
 * through to Keycloak, which validates them against the realm policy and stores them; the
 * application never stores a password.
 */
@Service
public class IdentityAccountService {

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloak;
    private final AuditService auditService;

    public IdentityAccountService(UserRepository userRepository, KeycloakAdminClient keycloak,
                                  AuditService auditService) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
        this.auditService = auditService;
    }

    /**
     * Assigns a temporary password the user must change at their next login, creating the
     * Keycloak account first if the user has none yet. Their open sessions are ended.
     */
    public void assignTemporaryPassword(Long userId, String password) {
        if (userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new BadRequestException("رمز عبور خودتان را از صفحه‌ی پروفایل تغییر دهید");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("کاربری با این شناسه پیدا نشد: " + userId));

        String subject = user.getKeycloakSubject();
        if (subject == null) {
            subject = keycloak.findUserIdByEmail(user.getEmail())
                    .orElseGet(() -> keycloak.createUser(UserMigrationService.representation(user)));
            user.setKeycloakSubject(subject);
            user.setPasswordHash(null);
            userRepository.save(user);
        }
        keycloak.setTemporaryPassword(subject, password);
        keycloak.logoutUser(subject);

        auditService.record(AuditEntry.success(AuditAction.PASSWORD_RESET_BY_ADMIN)
                .entityType("USER").entityId(userId).build());
    }
}
