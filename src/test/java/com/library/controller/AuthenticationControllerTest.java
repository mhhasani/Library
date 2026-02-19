package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.AuthResponse;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.dto.UserDTO;
import com.library.exception.BadRequestException;
import com.library.exception.UnauthorizedException;
import com.library.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Authentication Controller Tests")
class AuthenticationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserDTO userDTO;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .email("newuser@library.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .email("user@library.com")
                .password("password123")
                .build();

        userDTO = UserDTO.builder()
                .id(1L)
                .email("newuser@library.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .build();

        authResponse = AuthResponse.builder()
                .userId(1L)
                .email("newuser@library.com")
                .systemRole("USER")
                .accessToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                .refreshToken("refresh_token_123")
                .build();
    }

    @Test
    @DisplayName("Should register user successfully")
    void testRegisterSuccess() throws Exception {
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(userDTO);

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("newuser@library.com"));
    }

    @Test
    @DisplayName("Should return 400 when email already registered")
    void testRegisterEmailExists() throws Exception {
        doThrow(new BadRequestException("Email already registered"))
                .when(authenticationService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 with invalid register request")
    void testRegisterInvalidRequest() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login user successfully")
    void testLoginSuccess() throws Exception {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.email").value("newuser@library.com"));
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        doThrow(new UnauthorizedException("Invalid email or password"))
                .when(authenticationService).login(any(LoginRequest.class));

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 with invalid login request")
    void testLoginInvalidRequest() throws Exception {
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("")
                .password("")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshTokenSuccess() throws Exception {
        when(authenticationService.refreshToken("refresh_token_123")).thenReturn(authResponse);

        mockMvc.perform(post("/v1/auth/refresh")
                .header("Authorization", "Bearer refresh_token_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    @DisplayName("Should return 401 with invalid refresh token")
    void testRefreshTokenInvalid() throws Exception {
        doThrow(new UnauthorizedException("Invalid or expired refresh token"))
                .when(authenticationService).refreshToken("invalid_token");

        mockMvc.perform(post("/v1/auth/refresh")
                .header("Authorization", "Bearer invalid_token"))
                .andExpect(status().isUnauthorized());
    }
}
