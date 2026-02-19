package com.library.service;

import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserDTO;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.UnauthorizedException;
import com.library.repository.UserRepository;
import com.library.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public UserDTO register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)  // Auto-activate for now
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        return mapToUserDTO(user);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Get user
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            String accessToken = tokenProvider.generateAccessToken(user.getEmail(), user.getId());
            String refreshToken = tokenProvider.generateRefreshToken(user.getEmail(), user.getId());

            log.info("User logged in successfully: {}", user.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .systemRole(user.getSystemRole().toString())
                    .build();

        } catch (AuthenticationException ex) {
            log.error("Authentication failed for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String email = tokenProvider.extractEmail(refreshToken);
        Long userId = tokenProvider.extractUserId(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newAccessToken = tokenProvider.generateAccessToken(email, userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(email, userId);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .email(email)
                .systemRole(user.getSystemRole().toString())
                .build();
    }

    private UserDTO mapToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .systemRole(user.getSystemRole())
                .accountStatus(user.getAccountStatus())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
