package com.library.keycloak;

import com.library.entity.SecuritySettings;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Applies the application's security settings to the Keycloak realm, which enforces them
 * at login: lockout after failed attempts, session idle timeout, password history and
 * expiry, and whether a second factor is mandatory for everyone.
 */
@Component
public class KeycloakRealmSync {

    /** Realm attribute read by the library-condition-mfa-required authenticator. */
    static final String MFA_REQUIRED_ATTRIBUTE = "libraryMfaRequired";

    private static final Pattern HISTORY = Pattern.compile("passwordHistory\\(\\d+\\)");
    private static final Pattern EXPIRY = Pattern.compile("forceExpiredPasswordChange\\(\\d+\\)");

    private final KeycloakAdminClient client;

    public KeycloakRealmSync(KeycloakAdminClient client) {
        this.client = client;
    }

    @SuppressWarnings("unchecked")
    public void apply(SecuritySettings settings) {
        Map<String, Object> realm = client.getRealm();

        Map<String, Object> update = new HashMap<>();
        update.put("bruteForceProtected", true);
        update.put("permanentLockout", false);
        update.put("failureFactor", settings.getMaxFailedLogins());
        update.put("waitIncrementSeconds", settings.getLockoutMinutes() * 60);
        update.put("maxFailureWaitSeconds", settings.getLockoutMinutes() * 60);
        update.put("maxDeltaTimeSeconds", settings.getFailureResetMinutes() * 60);
        update.put("ssoSessionIdleTimeout", settings.getSessionIdleMinutes() * 60);
        update.put("passwordPolicy", passwordPolicy((String) realm.get("passwordPolicy"),
                settings.getPasswordHistory(), settings.getPasswordMaxAgeDays()));

        Map<String, Object> attributes = new HashMap<>();
        if (realm.get("attributes") instanceof Map<?, ?> existing) {
            attributes.putAll((Map<String, Object>) existing);
        }
        attributes.put(MFA_REQUIRED_ATTRIBUTE, String.valueOf(settings.isMfaRequired()));
        update.put("attributes", attributes);

        client.updateRealm(update);
    }

    /** Keeps every other rule of the policy and sets history/expiry to the given values. */
    static String passwordPolicy(String current, int history, int maxAgeDays) {
        String policy = current == null ? "" : current;
        policy = setRule(policy, HISTORY, "passwordHistory(" + history + ")");
        policy = setRule(policy, EXPIRY, "forceExpiredPasswordChange(" + maxAgeDays + ")");
        return policy;
    }

    private static String setRule(String policy, Pattern rule, String value) {
        if (rule.matcher(policy).find()) {
            return rule.matcher(policy).replaceFirst(value);
        }
        return policy.isBlank() ? value : policy + " and " + value;
    }
}
