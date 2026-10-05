package com.library.security;

import com.library.config.OidcProperties;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * Maps an authenticated identity-provider account to the local user row.
 *
 * <ol>
 *   <li>already linked: found by OIDC subject;</li>
 *   <li>existing local account not yet linked: matched by e-mail and linked;</li>
 *   <li>otherwise a new regular user is created (self-registration happens in Keycloak).</li>
 * </ol>
 * Roles and status stay under the application's control; a non-active account is refused
 * even though the password was correct.
 */
@Slf4j
@Service
public class UserProvisioningService {

    public static final String ACCOUNT_INACTIVE = "account_inactive";
    public static final String ACCOUNT_CONFLICT = "account_conflict";

    private final UserRepository userRepository;
    private final OidcProperties properties;

    public UserProvisioningService(UserRepository userRepository, OidcProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Transactional
    public User resolve(OidcUser identity) {
        String subject = identity.getSubject();
        String email = identity.getEmail() == null ? null : identity.getEmail().toLowerCase(Locale.ROOT);

        User user = userRepository.findByKeycloakSubject(subject)
                .or(() -> email == null ? Optional.empty() : userRepository.findByEmailIgnoreCase(email))
                .orElseGet(() -> create(identity, email));

        if (user.getKeycloakSubject() == null) {
            user.setKeycloakSubject(subject);
            user.setPasswordHash(null);
            user = userRepository.save(user);
            log.info("Linked existing user {} to identity provider account {}", user.getId(), subject);
        } else if (!user.getKeycloakSubject().equals(subject)) {
            throw failure(ACCOUNT_CONFLICT, "Local account is linked to another identity");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw failure(ACCOUNT_INACTIVE, "Account is not active");
        }
        return user;
    }

    private User create(OidcUser identity, String email) {
        if (email == null) {
            throw failure(ACCOUNT_CONFLICT, "Identity has no e-mail address");
        }
        boolean bootstrapSuperAdmin = email.equalsIgnoreCase(properties.bootstrapSuperAdminEmail())
                && userRepository.countBySystemRole(SystemRole.SUPER_ADMIN) == 0;
        User user = User.builder()
                .email(email)
                .keycloakSubject(identity.getSubject())
                .firstName(orDash(identity.getGivenName()))
                .lastName(orDash(identity.getFamilyName()))
                .systemRole(bootstrapSuperAdmin ? SystemRole.SUPER_ADMIN : SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        user = userRepository.save(user);
        log.info("Provisioned user {} from identity provider{}", user.getId(),
                bootstrapSuperAdmin ? " as the initial super admin" : "");
        return user;
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static OAuth2AuthenticationException failure(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }
}
