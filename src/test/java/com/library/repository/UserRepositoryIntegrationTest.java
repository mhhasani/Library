package com.library.repository;

import com.library.BaseIntegrationTest;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testCreateUser() {
        User savedUser = userRepository.save(testUser);
        
        assertNotNull(savedUser.getId());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("Test", savedUser.getFirstName());
    }

    @Test
    void testFindByEmail() {
        userRepository.save(testUser);
        
        Optional<User> found = userRepository.findByEmail("test@example.com");
        
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getFirstName());
    }

    @Test
    void testEmailUniqueness() {
        userRepository.save(testUser);
        
        User duplicateUser = User.builder()
                .email("test@example.com")
                .passwordHash("differenthash")
                .firstName("Duplicate")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        assertThrows(Exception.class, () -> userRepository.save(duplicateUser));
    }

    @Test
    void testUserExists() {
        userRepository.save(testUser);
        
        boolean exists = userRepository.existsByEmail("test@example.com");
        
        assertTrue(exists);
    }

    @Test
    void testUpdateUser() {
        User savedUser = userRepository.save(testUser);
        
        savedUser.setPhoneNumber("+9876543210");
        savedUser.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(savedUser);
        
        assertEquals("+9876543210", updatedUser.getPhoneNumber());
    }
}
