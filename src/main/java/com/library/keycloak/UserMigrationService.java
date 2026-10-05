package com.library.keycloak;

import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Moves accounts that predate Keycloak into the realm. Each user keeps their existing
 * password (the bcrypt hash is imported and verified by the realm's bcrypt provider),
 * must change it at the next login (so it meets the current policy), and the local hash
 * is removed once the account is linked. Idempotent: linked users are skipped.
 */
@Slf4j
@Service
public class UserMigrationService {

    private static final Pattern BCRYPT = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloak;

    public UserMigrationService(UserRepository userRepository, KeycloakAdminClient keycloak) {
        this.userRepository = userRepository;
        this.keycloak = keycloak;
    }

    /**
     * Links every not-yet-linked user. A user Keycloak rejects is skipped and retried on the
     * next run, so one bad record never blocks the others.
     *
     * @return number of users linked in this run
     */
    public int migratePendingUsers() {
        int migrated = 0;
        for (User user : userRepository.findByKeycloakSubjectIsNull()) {
            try {
                String subject = keycloak.findUserIdByEmail(user.getEmail())
                        .orElseGet(() -> keycloak.createUser(representation(user)));
                user.setKeycloakSubject(subject);
                user.setPasswordHash(null);
                userRepository.save(user);
                migrated++;
                log.info("Linked user {} to identity provider account {}", user.getId(), subject);
            } catch (KeycloakAdminException e) {
                log.warn("Could not migrate user {} to the identity provider: {}", user.getId(), e.getMessage());
            }
        }
        return migrated;
    }

    static Map<String, Object> representation(User user) {
        Map<String, Object> rep = new HashMap<>();
        rep.put("username", user.getEmail());
        rep.put("email", user.getEmail());
        rep.put("emailVerified", true);
        rep.put("firstName", user.getFirstName());
        rep.put("lastName", user.getLastName());
        rep.put("enabled", user.getAccountStatus() == AccountStatus.ACTIVE);
        rep.put("requiredActions", List.of("UPDATE_PASSWORD"));
        String hash = user.getPasswordHash();
        if (hash != null && BCRYPT.matcher(hash).matches()) {
            rep.put("credentials", List.of(Map.of(
                    "type", "password",
                    "credentialData", "{\"hashIterations\":" + hash.substring(4, 6) + ",\"algorithm\":\"bcrypt\"}",
                    "secretData", "{\"value\":\"" + hash + "\",\"salt\":\"\"}")));
        }
        return rep;
    }
}
