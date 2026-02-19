package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class AuthenticationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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
                .email("newuser@example.com")
                .password("SecurePass123!")
                .build();
    }

    @Test
    void testRegisterUserSuccess() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", equalTo("newuser@example.com")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void testRegisterUserMissingEmail() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .build();

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testRegisterUserDuplicateEmail() throws Exception {
        // Register first user
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try to register with same email
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testLoginUserSuccess() throws Exception {
        // Register user first
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login
        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", equalTo("newuser@example.com")))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void testLoginUserInvalidPassword() throws Exception {
        // Register user
        mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Try login with wrong password
        LoginRequest wrongPasswordRequest = LoginRequest.builder()
                .email("newuser@example.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testLoginUserNonExistentEmail() throws Exception {
        LoginRequest nonExistentRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("SomePassword123!")
                .build();

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonExistentRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testRefreshTokenSuccess() throws Exception {
        // Register to get tokens
        var response = mockMvc.perform(post("/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = response.getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(responseBody)
                .get("data")
                .get("refreshToken")
                .asText();

        // Use refresh token
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()));
    }

    @Test
    void testRefreshTokenInvalid() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("refreshToken", "invalid_token"))
                .andExpect(status().isUnauthorized());
    }
}
