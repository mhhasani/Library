package com.library.settings;

import com.library.audit.AuditEntry;
import com.library.audit.AuditService;
import com.library.config.OidcProperties;
import com.library.dto.UpdateSecuritySettingsRequest;
import com.library.entity.SecuritySettings;
import com.library.entity.enums.AuditAction;
import com.library.keycloak.KeycloakRealmSync;
import com.library.repository.SecuritySettingsRepository;
import com.library.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Reads and changes the security settings. A change is saved and pushed to Keycloak in
 * one transaction: if Keycloak rejects it or cannot be reached, nothing is changed, so the
 * stored settings always describe what is actually enforced.
 */
@Service
public class SecuritySettingsService {

    private final SecuritySettingsRepository repository;
    private final KeycloakRealmSync realmSync;
    private final AuditService auditService;
    private final boolean syncEnabled;

    private volatile SecuritySettings cached;

    public SecuritySettingsService(SecuritySettingsRepository repository, KeycloakRealmSync realmSync,
                                   AuditService auditService, OidcProperties oidcProperties) {
        this.repository = repository;
        this.realmSync = realmSync;
        this.auditService = auditService;
        this.syncEnabled = oidcProperties.adminSyncEnabled();
    }

    /** Current settings (cached; read on every request by the session filter). */
    public SecuritySettings get() {
        SecuritySettings current = cached;
        if (current == null) {
            current = repository.findById(SecuritySettings.SINGLETON_ID)
                    .orElseThrow(() -> new IllegalStateException("security_settings row is missing"));
            cached = current;
        }
        return current;
    }

    @Transactional
    public SecuritySettings update(UpdateSecuritySettingsRequest request) {
        SecuritySettings settings = repository.findById(SecuritySettings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("security_settings row is missing"));
        String before = describe(settings);

        settings.setMaxFailedLogins(request.maxFailedLogins());
        settings.setLockoutMinutes(request.lockoutMinutes());
        settings.setFailureResetMinutes(request.failureResetMinutes());
        settings.setSessionIdleMinutes(request.sessionIdleMinutes());
        settings.setPasswordHistory(request.passwordHistory());
        settings.setPasswordMaxAgeDays(request.passwordMaxAgeDays());
        settings.setMfaRequired(request.mfaRequired());
        settings.setReauthWindowMinutes(request.reauthWindowMinutes());
        settings.setSensitiveOperationSet(request.sensitiveOperations());
        settings.setUpdatedAt(LocalDateTime.now());
        settings.setUpdatedBy(SecurityUtils.getCurrentUserId());
        SecuritySettings saved = repository.saveAndFlush(settings);

        if (syncEnabled) {
            realmSync.apply(saved);
        }
        // Reloaded from the committed row on next use (a rollback leaves the cache untouched)
        cached = null;
        auditService.record(AuditEntry.success(AuditAction.SECURITY_SETTINGS_CHANGE)
                .entityType("SECURITY_SETTINGS").entityId(saved.getId())
                .details(before + " -> " + describe(saved)).build());
        return saved;
    }

    private static String describe(SecuritySettings s) {
        return String.format("{failedLogins=%d, lockout=%dm, reset=%dm, idle=%dm, history=%d, maxAge=%dd, mfa=%s, reauth=%dm, sensitive=%s}",
                s.getMaxFailedLogins(), s.getLockoutMinutes(), s.getFailureResetMinutes(), s.getSessionIdleMinutes(),
                s.getPasswordHistory(), s.getPasswordMaxAgeDays(), s.isMfaRequired(), s.getReauthWindowMinutes(),
                s.getSensitiveOperations());
    }
}
