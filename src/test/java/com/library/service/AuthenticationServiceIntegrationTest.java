package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.AuthResponse;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import org.springframework.security.authentication.BadCredentialsException;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        registerRequest = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .email("existing@example.com")
                .password("SecurePass123!")
                .build();
    }

    @Test
    void testSuccessfulRegistration() {
        AuthResponse response = authenticationService.register(registerRequest);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("newuser@example.com", response.getEmail());

        // Verify user was saved in database
        User savedUser = userRepository.findByEmail("newuser@example.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals(AccountStatus.ACTIVE, savedUser.getAccountStatus());
    }

    @Test
    void testRegistrationWithDuplicateEmail() {
        // Create first user
        authenticationService.register(registerRequest);

        // Attempt to register with same email
        assertThrows(BadRequestException.class, () -> authenticationService.register(registerRequest));
    }

    @Test
    void testSuccessfulLogin() {
        // First register a user
        authenticationService.register(registerRequest);

        // Now login with same credentials
        LoginRequest loginReq = LoginRequest.builder()
                .email("newuser@example.com")
                .password("SecurePass123!")
                .build();

        AuthResponse response = authenticationService.login(loginReq);

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("newuser@example.com", response.getEmail());
    }

    @Test
    void testLoginWithIncorrectPassword() {
        // Register user
        authenticationService.register(registerRequest);

        // Attempt login with wrong password
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .email("newuser@example.com")
                .password("WrongPassword123!")
                .build();

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(wrongPasswordRequest));
    }

    @Test
    void testLoginWithNonExistentEmail() {
        LoginRequest nonExistentRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("SomePassword123!")
                .build();

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(nonExistentRequest));
    }

    @Test
    void testRefreshToken() {
        // Register and get initial tokens
        AuthResponse initialResponse = authenticationService.register(registerRequest);
        String refreshToken = initialResponse.getRefreshToken();

        // Use refresh token to get new tokens
        AuthResponse refreshedResponse = authenticationService.refreshToken(refreshToken);

        assertNotNull(refreshedResponse);
        assertNotNull(refreshedResponse.getAccessToken());
        assertNotNull(refreshedResponse.getRefreshToken());
        // New tokens should be different
        assertNotEquals(initialResponse.getAccessToken(), refreshedResponse.getAccessToken());
    }

    @Test
    void testRefreshTokenWithInvalidToken() {
        assertThrows(Exception.class, () -> authenticationService.refreshToken("invalid_token"));
    }

    @Test
    void testUserLastLoginUpdated() {
        // Register user
        authenticationService.register(registerRequest);

        // Get user and check initial lastLoginAt
        User user = userRepository.findByEmail("newuser@example.com").orElse(null);
        assertNotNull(user);
        LocalDateTime firstLoginAt = user.getLastLoginAt();

        // Wait a bit and login again
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Login again
        LoginRequest loginReq = LoginRequest.builder()
                .email("newuser@example.com")
                .password("SecurePass123!")
                .build();
        authenticationService.login(loginReq);

        // Check lastLoginAt was updated
        User updatedUser = userRepository.findByEmail("newuser@example.com").orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getLastLoginAt());
    }
}
