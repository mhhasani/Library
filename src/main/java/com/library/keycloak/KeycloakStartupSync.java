package com.library.keycloak;

import com.library.settings.SecuritySettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Brings Keycloak in line with the application once it is reachable (it may start after
 * the application): pushes the stored security settings and links legacy accounts. Retries
 * until the settings have been applied; account migration runs on every pass and is a
 * no-op once everyone is linked.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.oidc", name = "admin-sync-enabled", havingValue = "true", matchIfMissing = true)
public class KeycloakStartupSync {

    private final SecuritySettingsService settingsService;
    private final KeycloakRealmSync realmSync;
    private final UserMigrationService migrationService;

    private volatile boolean settingsApplied;

    public KeycloakStartupSync(SecuritySettingsService settingsService, KeycloakRealmSync realmSync,
                               UserMigrationService migrationService) {
        this.settingsService = settingsService;
        this.realmSync = realmSync;
        this.migrationService = migrationService;
    }

    @Scheduled(initialDelayString = "PT20S", fixedDelayString = "PT2M")
    public void synchronize() {
        try {
            if (!settingsApplied) {
                realmSync.apply(settingsService.get());
                settingsApplied = true;
                log.info("Security settings applied to the identity provider");
            }
            int migrated = migrationService.migratePendingUsers();
            if (migrated > 0) {
                log.info("Migrated {} account(s) to the identity provider", migrated);
            }
        } catch (KeycloakAdminException e) {
            log.warn("Identity provider not ready ({}); will retry", e.getMessage());
        }
    }
}
