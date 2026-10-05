package com.library.keycloak;

import com.library.entity.SecuritySettings;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Keycloak synchronization and account migration")
class KeycloakIntegrationLogicTest {

    @Test
    @DisplayName("Settings are mapped onto the realm, keeping the rest of the password policy")
    @SuppressWarnings("unchecked")
    void realmSyncMapsSettings() {
        KeycloakAdminClient client = mock(KeycloakAdminClient.class);
        Map<String, Object> realm = new HashMap<>();
        realm.put("passwordPolicy", "length(8) and passwordHistory(3) and forceExpiredPasswordChange(90) and digits(1)");
        realm.put("attributes", Map.of("other", "kept"));
        when(client.getRealm()).thenReturn(realm);

        SecuritySettings settings = SecuritySettings.builder().maxFailedLogins(4).lockoutMinutes(20)
                .failureResetMinutes(60).sessionIdleMinutes(15).passwordHistory(2).passwordMaxAgeDays(45)
                .mfaRequired(false).build();
        new KeycloakRealmSync(client).apply(settings);

        ArgumentCaptor<Map<String, Object>> update = ArgumentCaptor.forClass(Map.class);
        verify(client).updateRealm(update.capture());
        Map<String, Object> sent = update.getValue();
        assertThat(sent).containsEntry("failureFactor", 4).containsEntry("waitIncrementSeconds", 1200)
                .containsEntry("maxDeltaTimeSeconds", 3600).containsEntry("ssoSessionIdleTimeout", 900);
        assertThat((String) sent.get("passwordPolicy")).isEqualTo(
                "length(8) and passwordHistory(2) and forceExpiredPasswordChange(45) and digits(1)");
        assertThat((Map<String, Object>) sent.get("attributes"))
                .containsEntry("other", "kept").containsEntry("libraryMfaRequired", "false");
    }

    @Test
    @DisplayName("Missing policy rules are appended")
    void policyRulesAppended() {
        assertThat(KeycloakRealmSync.passwordPolicy("length(8)", 3, 90))
                .isEqualTo("length(8) and passwordHistory(3) and forceExpiredPasswordChange(90)");
    }

    @Test
    @DisplayName("Legacy accounts are imported with their bcrypt hash and must change the password")
    @SuppressWarnings("unchecked")
    void migrationRepresentation() {
        User user = User.builder().email("a@x.ir").firstName("A").lastName("B")
                .accountStatus(AccountStatus.ACTIVE)
                .passwordHash("$2a$10$X7pZ9Cqc8gnA5zWGqnQqeOL4tsL.Yr9/4PAlAKxQrLxdvOS.bterK").build();

        Map<String, Object> rep = UserMigrationService.representation(user);

        assertThat(rep).containsEntry("username", "a@x.ir").containsEntry("enabled", true);
        assertThat((List<String>) rep.get("requiredActions")).containsExactly("UPDATE_PASSWORD");
        Map<String, String> credential = ((List<Map<String, String>>) rep.get("credentials")).get(0);
        assertThat(credential.get("credentialData")).contains("\"algorithm\":\"bcrypt\"").contains("\"hashIterations\":10");
        assertThat(credential.get("secretData")).contains(user.getPasswordHash());
    }

    @Test
    @DisplayName("Non-bcrypt or missing hashes are not imported as credentials")
    void noCredentialWithoutBcrypt() {
        User user = User.builder().email("b@x.ir").firstName("A").lastName("B")
                .accountStatus(AccountStatus.SUSPENDED).passwordHash("plain").build();

        Map<String, Object> rep = UserMigrationService.representation(user);

        assertThat(rep).doesNotContainKey("credentials").containsEntry("enabled", false);
    }
}
