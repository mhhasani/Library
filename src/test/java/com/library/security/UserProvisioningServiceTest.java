package com.library.security;

import com.library.BaseIntegrationTest;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@DisplayName("Linking identity-provider accounts to local users")
class UserProvisioningServiceTest extends BaseIntegrationTest {

    @Autowired private UserProvisioningService provisioning;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("An existing local account is linked by e-mail and its legacy hash removed")
    void linksExistingAccountByEmail() {
        User existing = userRepository.save(user("Ali@X.ir", SystemRole.SYSTEM_ADMIN, AccountStatus.ACTIVE));

        User resolved = provisioning.resolve(identity("kc-1", "ali@x.ir"));

        assertThat(resolved.getId()).isEqualTo(existing.getId());
        assertThat(resolved.getKeycloakSubject()).isEqualTo("kc-1");
        assertThat(resolved.getPasswordHash()).isNull();
        assertThat(resolved.getSystemRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("A new identity becomes a regular user")
    void createsRegularUser() {
        User created = provisioning.resolve(identity("kc-2", "new@x.ir"));

        assertThat(created.getSystemRole()).isEqualTo(SystemRole.USER);
        assertThat(created.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(created.getFirstName()).isEqualTo("Given");
    }

    @Test
    @DisplayName("The configured bootstrap account becomes super admin only while none exists")
    void bootstrapSuperAdmin() {
        User root = provisioning.resolve(identity("kc-root", "root@library.test"));
        assertThat(root.getSystemRole()).isEqualTo(SystemRole.SUPER_ADMIN);
    }

    @Test
    @DisplayName("A suspended account is refused even with valid credentials")
    void inactiveAccountRefused() {
        userRepository.save(user("blocked@x.ir", SystemRole.USER, AccountStatus.SUSPENDED));

        assertThatThrownBy(() -> provisioning.resolve(identity("kc-3", "blocked@x.ir")))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(UserProvisioningService.ACCOUNT_INACTIVE));
    }

    @Test
    @DisplayName("A local account already linked to another identity is never taken over")
    void conflictingSubjectRefused() {
        User linked = user("owner@x.ir", SystemRole.USER, AccountStatus.ACTIVE);
        linked.setKeycloakSubject("kc-original");
        userRepository.save(linked);

        assertThatThrownBy(() -> provisioning.resolve(identity("kc-attacker", "owner@x.ir")))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static User user(String email, SystemRole role, AccountStatus status) {
        return User.builder().email(email).passwordHash("$2a$10$legacy").firstName("F").lastName("L")
                .systemRole(role).accountStatus(status).build();
    }

    private static OidcUser identity(String subject, String email) {
        OidcIdToken token = new OidcIdToken("t", Instant.now(), Instant.now().plusSeconds(60), Map.of(
                "sub", subject, "email", email, "given_name", "Given", "family_name", "Family"));
        return new DefaultOidcUser(List.of(), token, "email");
    }
}
