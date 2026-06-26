package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.UserDTO;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("User Controller Tests")
class UserControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private UserService userService;

    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userDTO = UserDTO.builder()
                .id(1L).email("user@test.com").firstName("Ali").lastName("Ahmadi")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE).build();
    }

    // ── GET /v1/users/me ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Authenticated user gets own profile — 200")
    void getProfile_authenticated_ok() throws Exception {
        when(userService.getCurrentUserProfile()).thenReturn(userDTO);

        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.firstName").value("Ali"));
    }

    @Test
    @DisplayName("Unauthenticated request to GET /v1/users/me — 401")
    void getProfile_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
    }

    // ── PUT /v1/users/me ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Authenticated user updates own profile — 200")
    void updateProfile_authenticated_ok() throws Exception {
        UserDTO updated = UserDTO.builder().id(1L).email("user@test.com")
                .firstName("Mohammad").lastName("Rezaei")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE).build();
        when(userService.updateProfile(any())).thenReturn(updated);

        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("firstName", "Mohammad", "lastName", "Rezaei"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Mohammad"))
                .andExpect(jsonPath("$.data.lastName").value("Rezaei"));
    }

    @Test
    @DisplayName("Unauthenticated request to PUT /v1/users/me — 401")
    void updateProfile_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("firstName", "X", "lastName", "Y"))))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /v1/users/me/password ──────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Change password with correct current password — 200")
    void changePassword_success() throws Exception {
        doNothing().when(userService).changePassword(any());

        mockMvc.perform(put("/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "old12345", "newPassword", "newPass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Change password with wrong current password — 400")
    void changePassword_wrongCurrent_badRequest() throws Exception {
        doThrow(new BadRequestException("رمز عبور فعلی نادرست است"))
                .when(userService).changePassword(any());

        mockMvc.perform(put("/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "wrong123", "newPassword", "newPass123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unauthenticated request to PUT /v1/users/me/password — 401")
    void changePassword_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(put("/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("currentPassword", "x", "newPassword", "tooShort"))))
                .andExpect(status().isUnauthorized());
    }
}
