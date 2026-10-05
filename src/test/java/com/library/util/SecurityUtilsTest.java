package com.library.util;

import com.library.entity.User;
import com.library.entity.enums.SystemRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityUtils Tests")
class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("getCurrentUserId returns id when principal is AppUserDetails")
    void getCurrentUserId_withAppUserDetails() {
        User user = User.builder().id(42L).email("a@b.com").systemRole(SystemRole.USER).build();
        SecurityTestUtils.setSecurityContext(user, "USER");

        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getCurrentUserId returns null when there is no authentication")
    void getCurrentUserId_noAuthentication() {
        SecurityTestUtils.clearSecurityContext();

        assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    @DisplayName("getCurrentUserId returns null when principal is not AppUserDetails")
    void getCurrentUserId_nonUserDetailsPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("plainPrincipal", "creds", Collections.emptyList()));

        assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    @DisplayName("getCurrentUserEmail returns authentication name when authenticated")
    void getCurrentUserEmail_withAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("someone@example.com", "creds", Collections.emptyList()));

        assertThat(SecurityUtils.getCurrentUserEmail()).isEqualTo("someone@example.com");
    }

    @Test
    @DisplayName("getCurrentUserEmail returns null when there is no authentication")
    void getCurrentUserEmail_noAuthentication() {
        SecurityTestUtils.clearSecurityContext();

        assertThat(SecurityUtils.getCurrentUserEmail()).isNull();
    }

    @Test
    @DisplayName("hasRole returns true when authorities contain the role")
    void hasRole_true() {
        User user = User.builder().id(1L).email("a@b.com").systemRole(SystemRole.USER).build();
        SecurityTestUtils.setSecurityContext(user, "ADMIN");

        assertThat(SecurityUtils.hasRole("ADMIN")).isTrue();
    }

    @Test
    @DisplayName("hasRole returns false when authorities do not contain the role")
    void hasRole_false() {
        User user = User.builder().id(1L).email("a@b.com").systemRole(SystemRole.USER).build();
        SecurityTestUtils.setSecurityContext(user, "USER");

        assertThat(SecurityUtils.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("hasRole returns false when there is no authentication")
    void hasRole_noAuthentication() {
        SecurityTestUtils.clearSecurityContext();

        assertThat(SecurityUtils.hasRole("ADMIN")).isFalse();
    }

    @Test
    @DisplayName("hasRole('SYSTEM_ADMIN') returns true for a SUPER_ADMIN user (role hierarchy)")
    void hasRole_superAdminSatisfiesSystemAdminCheck() {
        User user = User.builder().id(1L).email("a@b.com").systemRole(SystemRole.SUPER_ADMIN).build();
        SecurityTestUtils.setSecurityContext(user, "SUPER_ADMIN");

        assertThat(SecurityUtils.hasRole("SYSTEM_ADMIN")).isTrue();
        assertThat(SecurityUtils.hasRole("USER")).isTrue();
    }

    @Test
    @DisplayName("hasRole('USER') returns true for a SYSTEM_ADMIN user (role hierarchy)")
    void hasRole_systemAdminSatisfiesUserCheck() {
        User user = User.builder().id(1L).email("a@b.com").systemRole(SystemRole.SYSTEM_ADMIN).build();
        SecurityTestUtils.setSecurityContext(user, "SYSTEM_ADMIN");

        assertThat(SecurityUtils.hasRole("USER")).isTrue();
        assertThat(SecurityUtils.hasRole("SUPER_ADMIN")).isFalse();
    }
}
