package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.UpdateProfileRequest;
import com.library.dto.UserDTO;
import com.library.entity.Notification;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.NotificationType;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private NotificationRepository notificationRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("testuser@library.com")
                .passwordHash("legacy-hash")
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

    // ── Last-admin protection ────────────────────────────────────────────────

    @Test
    @DisplayName("Cannot suspend the only active SYSTEM_ADMIN")
    void testUpdateStatusCannotSuspendLastAdmin() {
        User admin = User.builder()
                .email("sysadmin@lib.com")
                .passwordHash("legacy-hash")
                .firstName("Sys").lastName("Admin")
                .systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin = userRepository.save(admin);

        // Caller is a plain USER (not counted as admin) — service-layer guard is role-agnostic
        User caller = User.builder()
                .email("caller@lib.com")
                .passwordHash("legacy-hash")
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
                .email("admin1@lib.com").passwordHash("legacy-hash")
                .firstName("A1").lastName("A").systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin1 = userRepository.save(admin1);

        User admin2 = User.builder()
                .email("admin2@lib.com").passwordHash("legacy-hash")
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
                .email("sysadmin2@lib.com").passwordHash("legacy-hash")
                .firstName("Sys").lastName("Admin").systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        admin = userRepository.save(admin);

        // Caller is a plain USER — the service-layer guard does not check the caller's role
        User caller = User.builder()
                .email("caller2@lib.com").passwordHash("legacy-hash")
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

    // ── Not-found branches ──────────────────────────────────────────────────

    @Test
    @DisplayName("getCurrentUserProfile throws when current user no longer exists")
    void testGetCurrentUserProfileNotFound() {
        User ghost = User.builder()
                .id(999999L)
                .email("ghost@library.com")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        SecurityTestUtils.setSecurityContext(ghost, "USER");

        assertThatThrownBy(() -> userService.getCurrentUserProfile())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کاربر پیدا نشد");
    }

    @Test
    @DisplayName("updateProfile throws when current user no longer exists")
    void testUpdateProfileNotFound() {
        User ghost = User.builder()
                .id(999999L)
                .email("ghost2@library.com")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        SecurityTestUtils.setSecurityContext(ghost, "USER");

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("X").lastName("Y").build();

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کاربر پیدا نشد");
    }

    @Test
    @DisplayName("updateProfile updates deliveryAddress and internalExtension too")
    void testUpdateProfileAllFields() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("سارا")
                .lastName("محمدی")
                .phoneNumber("09121112233")
                .deliveryAddress("تهران، خیابان آزادی")
                .internalExtension("101")
                .build();

        UserDTO result = userService.updateProfile(request);

        assertThat(result.getDeliveryAddress()).isEqualTo("تهران، خیابان آزادی");
        assertThat(result.getInternalExtension()).isEqualTo("101");
    }

    // ── getUsers / getUsersPaged ─────────────────────────────────────────────

    @Test
    @DisplayName("getUsers(null) returns all users")
    void testGetUsersNoFilter() {
        List<UserDTO> users = userService.getUsers(null);
        assertThat(users).extracting(UserDTO::getEmail).contains("testuser@library.com");
    }

    @Test
    @DisplayName("getUsers(status) filters by account status")
    void testGetUsersWithStatusFilter() {
        User suspended = User.builder()
                .email("suspended@lib.com").passwordHash("legacy-hash")
                .firstName("S").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.SUSPENDED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        userRepository.save(suspended);

        List<UserDTO> activeUsers = userService.getUsers(AccountStatus.ACTIVE);
        assertThat(activeUsers).extracting(UserDTO::getEmail).contains("testuser@library.com");
        assertThat(activeUsers).extracting(UserDTO::getEmail).doesNotContain("suspended@lib.com");

        List<UserDTO> suspendedUsers = userService.getUsers(AccountStatus.SUSPENDED);
        assertThat(suspendedUsers).extracting(UserDTO::getEmail).contains("suspended@lib.com");
    }

    @Test
    @DisplayName("getUsersPaged with no filters returns all users")
    void testGetUsersPagedNoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserDTO> page = userService.getUsersPaged(null, null, pageable);
        assertThat(page.getContent()).extracting(UserDTO::getEmail).contains("testuser@library.com");
    }

    @Test
    @DisplayName("getUsersPaged filters by status")
    void testGetUsersPagedByStatus() {
        User suspended = User.builder()
                .email("suspended2@lib.com").passwordHash("legacy-hash")
                .firstName("S").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.SUSPENDED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        userRepository.save(suspended);

        Page<UserDTO> page = userService.getUsersPaged(AccountStatus.SUSPENDED, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(UserDTO::getEmail).containsExactly("suspended2@lib.com");
    }

    @Test
    @DisplayName("getUsersPaged searches by email/name/phone case-insensitively")
    void testGetUsersPagedBySearch() {
        Page<UserDTO> byEmail = userService.getUsersPaged(null, "TESTUSER@LIBRARY", PageRequest.of(0, 10));
        assertThat(byEmail.getContent()).extracting(UserDTO::getEmail).contains("testuser@library.com");

        Page<UserDTO> byFirstName = userService.getUsersPaged(null, "علی", PageRequest.of(0, 10));
        assertThat(byFirstName.getContent()).extracting(UserDTO::getEmail).contains("testuser@library.com");

        Page<UserDTO> byPhone = userService.getUsersPaged(null, "09121234567", PageRequest.of(0, 10));
        assertThat(byPhone.getContent()).extracting(UserDTO::getEmail).contains("testuser@library.com");

        Page<UserDTO> noMatch = userService.getUsersPaged(null, "no-such-user-xyz", PageRequest.of(0, 10));
        assertThat(noMatch.getContent()).isEmpty();
    }

    @Test
    @DisplayName("getUsersPaged combines status and search filters")
    void testGetUsersPagedStatusAndSearch() {
        Page<UserDTO> page = userService.getUsersPaged(AccountStatus.ACTIVE, "testuser", PageRequest.of(0, 10));
        assertThat(page.getContent()).extracting(UserDTO::getEmail).contains("testuser@library.com");
    }

    // ── updateUserStatus: not-found + SUPER_ADMIN branch + bypass ───────────

    @Test
    @DisplayName("updateUserStatus throws ResourceNotFoundException for unknown user id")
    void testUpdateUserStatusUserNotFound() {
        assertThatThrownBy(() -> userService.updateUserStatus(999999L, AccountStatus.SUSPENDED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کاربری با این شناسه پیدا نشد");
    }

    @Test
    @DisplayName("Cannot suspend the only active SUPER_ADMIN")
    void testUpdateStatusCannotSuspendLastActiveSuperAdmin() {
        User superAdmin = User.builder()
                .email("superadmin@lib.com").passwordHash("legacy-hash")
                .firstName("Super").lastName("Admin").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        superAdmin = userRepository.save(superAdmin);

        final Long superAdminId = superAdmin.getId();
        assertThatThrownBy(() -> userService.updateUserStatus(superAdminId, AccountStatus.SUSPENDED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("آخرین ادمین اصلی فعال");
    }

    @Test
    @DisplayName("Can suspend a SUPER_ADMIN when another active SUPER_ADMIN exists")
    void testUpdateStatusCanSuspendSuperAdminWhenMultiple() {
        User superAdmin1 = User.builder()
                .email("sa1@lib.com").passwordHash("legacy-hash")
                .firstName("SA1").lastName("A").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        superAdmin1 = userRepository.save(superAdmin1);

        User superAdmin2 = User.builder()
                .email("sa2@lib.com").passwordHash("legacy-hash")
                .firstName("SA2").lastName("A").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        userRepository.save(superAdmin2);

        final Long sa1Id = superAdmin1.getId();
        assertThatNoException().isThrownBy(() -> userService.updateUserStatus(sa1Id, AccountStatus.SUSPENDED));

        User updated = userRepository.findById(sa1Id).orElseThrow();
        assertThat(updated.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Can suspend a regular (non-admin) user")
    void testUpdateStatusRegularUserSuccess() {
        User regular = User.builder()
                .email("regular@lib.com").passwordHash("legacy-hash")
                .firstName("R").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        regular = userRepository.save(regular);

        final Long regularId = regular.getId();
        UserDTO result = userService.updateUserStatus(regularId, AccountStatus.SUSPENDED);
        assertThat(result.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Reactivating the only SUPER_ADMIN to ACTIVE bypasses the last-admin guard")
    void testUpdateStatusToActiveBypassesGuard() {
        User superAdmin = User.builder()
                .email("sa-reactivate@lib.com").passwordHash("legacy-hash")
                .firstName("SA").lastName("R").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.SUSPENDED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        superAdmin = userRepository.save(superAdmin);

        final Long saId = superAdmin.getId();
        assertThatNoException().isThrownBy(() -> userService.updateUserStatus(saId, AccountStatus.ACTIVE));
        User updated = userRepository.findById(saId).orElseThrow();
        assertThat(updated.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    // ── updateUserRole: not-found + SUPER_ADMIN branch + promotion path ─────

    @Test
    @DisplayName("updateUserRole throws ResourceNotFoundException for unknown user id")
    void testUpdateUserRoleUserNotFound() {
        assertThatThrownBy(() -> userService.updateUserRole(999999L, SystemRole.USER))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کاربری با این شناسه پیدا نشد");
    }

    @Test
    @DisplayName("Cannot demote the only SUPER_ADMIN")
    void testUpdateRoleCannotDemoteLastSuperAdmin() {
        User superAdmin = User.builder()
                .email("lastsuper@lib.com").passwordHash("legacy-hash")
                .firstName("Last").lastName("Super").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        superAdmin = userRepository.save(superAdmin);

        final Long saId = superAdmin.getId();
        assertThatThrownBy(() -> userService.updateUserRole(saId, SystemRole.USER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("آخرین ادمین اصلی را نمی‌توان تنزل رتبه داد");
    }

    @Test
    @DisplayName("Can demote a SUPER_ADMIN to SYSTEM_ADMIN when another SUPER_ADMIN exists")
    void testUpdateRoleCanDemoteSuperAdminWhenMultiple() {
        User superAdmin1 = User.builder()
                .email("multi-sa1@lib.com").passwordHash("legacy-hash")
                .firstName("M1").lastName("A").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        superAdmin1 = userRepository.save(superAdmin1);

        User superAdmin2 = User.builder()
                .email("multi-sa2@lib.com").passwordHash("legacy-hash")
                .firstName("M2").lastName("A").systemRole(SystemRole.SUPER_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        userRepository.save(superAdmin2);

        final Long sa1Id = superAdmin1.getId();
        UserDTO result = userService.updateUserRole(sa1Id, SystemRole.SYSTEM_ADMIN);
        assertThat(result.getSystemRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
    }

    @Test
    @DisplayName("Can promote a regular USER to SYSTEM_ADMIN")
    void testUpdateRolePromoteRegularToAdmin() {
        User regular = User.builder()
                .email("promote@lib.com").passwordHash("legacy-hash")
                .firstName("P").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        regular = userRepository.save(regular);

        final Long regularId = regular.getId();
        UserDTO result = userService.updateUserRole(regularId, SystemRole.SYSTEM_ADMIN);
        assertThat(result.getSystemRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
    }

    // ── notifications ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUserStatus notifies the affected user when status actually changes")
    void testUpdateUserStatus_notifiesUser() {
        User target = User.builder()
                .email("notify-status@lib.com").passwordHash("legacy-hash")
                .firstName("N").lastName("S").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        target = userRepository.save(target);

        userService.updateUserStatus(target.getId(), AccountStatus.SUSPENDED);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(target.getId());
        assertThat(notifs).anyMatch(n -> n.getType() == NotificationType.ACCOUNT_STATUS_CHANGED);
    }

    @Test
    @DisplayName("updateUserStatus does not notify when the status is unchanged")
    void testUpdateUserStatus_noChange_doesNotNotify() {
        User target = User.builder()
                .email("notify-status-nochange@lib.com").passwordHash("legacy-hash")
                .firstName("N").lastName("C").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        target = userRepository.save(target);

        userService.updateUserStatus(target.getId(), AccountStatus.ACTIVE);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(target.getId());
        assertThat(notifs).noneMatch(n -> n.getType() == NotificationType.ACCOUNT_STATUS_CHANGED);
    }

    @Test
    @DisplayName("updateUserRole notifies the affected user")
    void testUpdateUserRole_notifiesUser() {
        User regular = User.builder()
                .email("notify-role@lib.com").passwordHash("legacy-hash")
                .firstName("N").lastName("R").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        regular = userRepository.save(regular);

        userService.updateUserRole(regular.getId(), SystemRole.SYSTEM_ADMIN);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(regular.getId());
        assertThat(notifs).anyMatch(n -> n.getType() == NotificationType.SYSTEM_ROLE_CHANGED);
    }
}
