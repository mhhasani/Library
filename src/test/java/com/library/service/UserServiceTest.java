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
}
