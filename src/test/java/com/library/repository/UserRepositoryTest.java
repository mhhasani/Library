package com.library.repository;

import com.library.BaseIntegrationTest;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and retrieve user")
    void testSaveUser() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$encoded");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPhoneNumber("+1234567890");
        user.setSystemRole(SystemRole.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getSystemRole()).isEqualTo(SystemRole.USER);
    }

    @Test
    @DisplayName("Should find user by email")
    void testFindByEmail() {
        // Given
        User user = new User();
        user.setEmail("findme@example.com");
        user.setPasswordHash("$2a$10$encoded");
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setSystemRole(SystemRole.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // When
        var found = userRepository.findByEmail("findme@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Jane");
        assertThat(found.get().getLastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void testExistsByEmail() {
        // Given
        User user = new User();
        user.setEmail("exists@example.com");
        user.setPasswordHash("$2a$10$encoded");
        user.setFirstName("Bob");
        user.setLastName("Brown");
        user.setSystemRole(SystemRole.USER);
        user.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notexists@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should save system admin user")
    void testSaveSystemAdmin() {
        // Given
        User admin = new User();
        admin.setEmail("admin@system.com");
        admin.setPasswordHash("$2a$10$encoded");
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setSystemRole(SystemRole.SYSTEM_ADMIN);
        admin.setAccountStatus(AccountStatus.ACTIVE);

        // When
        User saved = userRepository.save(admin);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSystemRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);
    }
}
