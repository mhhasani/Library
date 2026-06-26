package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.ChangePasswordRequest;
import com.library.dto.UpdateProfileRequest;
import com.library.dto.UserDTO;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("User Service Tests")
class UserServiceTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("testuser@library.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("علی")
                .lastName("احمدی")
                .phoneNumber("09121234567")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);
        SecurityTestUtils.setSecurityContext(testUser, "USER");
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Should return current user profile")
    void testGetCurrentUserProfile() {
        UserDTO profile = userService.getCurrentUserProfile();

        assertThat(profile).isNotNull();
        assertThat(profile.getEmail()).isEqualTo("testuser@library.com");
        assertThat(profile.getFirstName()).isEqualTo("علی");
        assertThat(profile.getLastName()).isEqualTo("احمدی");
        assertThat(profile.getPhoneNumber()).isEqualTo("09121234567");
    }

    @Test
    @DisplayName("Should update profile successfully")
    void testUpdateProfileSuccess() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("محمد")
                .lastName("رضایی")
                .phoneNumber("09359876543")
                .build();

        UserDTO result = userService.updateProfile(request);

        assertThat(result.getFirstName()).isEqualTo("محمد");
        assertThat(result.getLastName()).isEqualTo("رضایی");
        assertThat(result.getPhoneNumber()).isEqualTo("09359876543");
        assertThat(result.getEmail()).isEqualTo("testuser@library.com");
    }

    @Test
    @DisplayName("Should update profile without phone number")
    void testUpdateProfileWithoutPhone() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("محمد")
                .lastName("رضایی")
                .phoneNumber(null)
                .build();

        UserDTO result = userService.updateProfile(request);

        assertThat(result.getFirstName()).isEqualTo("محمد");
        assertThat(result.getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("Should change password successfully")
    void testChangePasswordSuccess() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("password123")
                .newPassword("newPassword456")
                .build();

        assertThatNoException().isThrownBy(() -> userService.changePassword(request));

        // Verify new password is set
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("newPassword456", updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("password123", updated.getPasswordHash())).isFalse();
    }

    @Test
    @DisplayName("Should throw BadRequestException when current password is wrong")
    void testChangePasswordWrongCurrentPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword456")
                .build();

        assertThatThrownBy(() -> userService.changePassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("رمز عبور فعلی نادرست است");
    }

    // ── Last-admin protection ────────────────────────────────────────────────

    @Test
    @DisplayName("Cannot suspend the only active SYSTEM_ADMIN")
    void testUpdateStatusCannotSuspendLastAdmin() {
        User admin = User.builder()
                .email("sysadmin@lib.com")
                .passwordHash(passwordEncoder.encode("pass"))
                .firstName("Sys").lastName("Admin")
                .systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin = userRepository.save(admin);

        // Caller is a plain USER (not counted as admin) — service-layer guard is role-agnostic
        User caller = User.builder()
                .email("caller@lib.com")
                .passwordHash(passwordEncoder.encode("pass"))
                .firstName("Caller").lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        caller = userRepository.save(caller);
        SecurityTestUtils.setSecurityContext(caller, "USER");

        final Long adminId = admin.getId();
        assertThatThrownBy(() -> userService.updateUserStatus(adminId, AccountStatus.SUSPENDED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("آخرین مدیر سیستم");
    }

    @Test
    @DisplayName("Can suspend a SYSTEM_ADMIN when another one exists")
    void testUpdateStatusCanSuspendNonLastAdmin() {
        // Two admins — suspending one should succeed
        User admin1 = User.builder()
                .email("admin1@lib.com").passwordHash(passwordEncoder.encode("pass"))
                .firstName("A1").lastName("A").systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin1 = userRepository.save(admin1);

        User admin2 = User.builder()
                .email("admin2@lib.com").passwordHash(passwordEncoder.encode("pass"))
                .firstName("A2").lastName("A").systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin2 = userRepository.save(admin2);
        SecurityTestUtils.setSecurityContext(admin2, "SYSTEM_ADMIN");

        final Long admin1Id = admin1.getId();
        assertThatNoException().isThrownBy(() -> userService.updateUserStatus(admin1Id, AccountStatus.SUSPENDED));
    }

    @Test
    @DisplayName("Cannot demote the only SYSTEM_ADMIN to USER")
    void testUpdateRoleCannotDemoteLastAdmin() {
        User admin = User.builder()
                .email("sysadmin2@lib.com").passwordHash(passwordEncoder.encode("pass"))
                .firstName("Sys").lastName("Admin").systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin = userRepository.save(admin);

        // Caller is a plain USER — the service-layer guard does not check the caller's role
        User caller = User.builder()
                .email("caller2@lib.com").passwordHash(passwordEncoder.encode("pass"))
                .firstName("C").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        caller = userRepository.save(caller);
        SecurityTestUtils.setSecurityContext(caller, "USER");

        final Long adminId = admin.getId();
        assertThatThrownBy(() -> userService.updateUserRole(adminId, SystemRole.USER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("حداقل یک مدیر سیستم");
    }

    @Test
    @DisplayName("Cannot change own status")
    void testUpdateStatusCannotChangeSelf() {
        SecurityTestUtils.setSecurityContext(testUser, "USER");
        final Long selfId = testUser.getId();
        assertThatThrownBy(() -> userService.updateUserStatus(selfId, AccountStatus.SUSPENDED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("خودتان");
    }

    @Test
    @DisplayName("Cannot change own role")
    void testUpdateRoleCannotChangeSelf() {
        SecurityTestUtils.setSecurityContext(testUser, "USER");
        final Long selfId = testUser.getId();
        assertThatThrownBy(() -> userService.updateUserRole(selfId, SystemRole.SYSTEM_ADMIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("خودتان");
    }
}
