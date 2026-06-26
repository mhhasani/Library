package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LibraryDTO;
import com.library.dto.UserDTO;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.service.LibraryService;
import com.library.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin User Controller Tests")
class AdminUserControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private UserService userService;
    @MockBean  private LibraryService libraryService;

    private UserDTO activeUser;
    private UserDTO suspendedUser;

    @BeforeEach
    void setUp() {
        activeUser = UserDTO.builder()
                .id(2L).email("user@test.com").firstName("Ali").lastName("Ahmadi")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE).build();
        suspendedUser = UserDTO.builder()
                .id(2L).email("user@test.com")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.SUSPENDED).build();
    }

    // ── GET /v1/admin/users ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin gets paginated users — 200")
    void getUsers_adminSuccess() throws Exception {
        Page<UserDTO> page = new PageImpl<>(List.of(activeUser), PageRequest.of(0, 12), 1);
        when(userService.getUsersPaged(isNull(), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].email").value("user@test.com"))
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin filters users by ACTIVE status — 200")
    void getUsers_filteredByStatus() throws Exception {
        Page<UserDTO> page = new PageImpl<>(List.of(activeUser), PageRequest.of(0, 12), 1);
        when(userService.getUsersPaged(eq(AccountStatus.ACTIVE), isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/admin/users").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin searches users by query — 200")
    void getUsers_withSearch() throws Exception {
        Page<UserDTO> page = new PageImpl<>(List.of(activeUser), PageRequest.of(0, 12), 1);
        when(userService.getUsersPaged(isNull(), eq("ali"), any())).thenReturn(page);

        mockMvc.perform(get("/v1/admin/users").param("search", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user gets 403 on /admin/users")
    void getUsers_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/v1/admin/users")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/users — 401")
    void getUsers_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/admin/users")).andExpect(status().isUnauthorized());
    }

    // ── PATCH /v1/admin/users/{id}/status ──────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin suspends a user — 200")
    void updateStatus_suspend_success() throws Exception {
        when(userService.updateUserStatus(2L, AccountStatus.SUSPENDED)).thenReturn(suspendedUser);

        mockMvc.perform(patch("/v1/admin/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin activates a user — 200")
    void updateStatus_activate_success() throws Exception {
        when(userService.updateUserStatus(2L, AccountStatus.ACTIVE)).thenReturn(activeUser);

        mockMvc.perform(patch("/v1/admin/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin cannot change own status — 400")
    void updateStatus_selfChange_badRequest() throws Exception {
        doThrow(new BadRequestException("نمی‌توانید وضعیت حساب خودتان را تغییر دهید"))
                .when(userService).updateUserStatus(eq(2L), any());

        mockMvc.perform(patch("/v1/admin/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Cannot suspend last active system admin — 400")
    void updateStatus_lastAdmin_badRequest() throws Exception {
        doThrow(new BadRequestException("آخرین مدیر سیستم فعال را نمی‌توان تعلیق یا حذف کرد"))
                .when(userService).updateUserStatus(eq(2L), eq(AccountStatus.SUSPENDED));

        mockMvc.perform(patch("/v1/admin/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("User not found for status update — 404")
    void updateStatus_userNotFound_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("کاربری با این شناسه پیدا نشد: 999"))
                .when(userService).updateUserStatus(999L, AccountStatus.SUSPENDED);

        mockMvc.perform(patch("/v1/admin/users/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user cannot update status — 403")
    void updateStatus_regularUser_forbidden() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /v1/admin/users/{id}/role ────────────────────────────────────

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super admin promotes user to SYSTEM_ADMIN — 200")
    void updateRole_promote_success() throws Exception {
        UserDTO promoted = UserDTO.builder().id(2L).systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE).build();
        when(userService.updateUserRole(2L, SystemRole.SYSTEM_ADMIN)).thenReturn(promoted);

        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SYSTEM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemRole").value("SYSTEM_ADMIN"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super admin demotes another admin to USER — 200")
    void updateRole_demote_success() throws Exception {
        when(userService.updateUserRole(2L, SystemRole.USER)).thenReturn(activeUser);

        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemRole").value("USER"));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super admin cannot change own role — 400")
    void updateRole_selfChange_badRequest() throws Exception {
        doThrow(new BadRequestException("نمی‌توانید نقش خودتان را تغییر دهید"))
                .when(userService).updateUserRole(eq(2L), any());

        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Cannot demote last system admin — 400")
    void updateRole_lastAdmin_badRequest() throws Exception {
        doThrow(new BadRequestException("حداقل یک مدیر سیستم باید وجود داشته باشد"))
                .when(userService).updateUserRole(eq(2L), eq(SystemRole.USER));

        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("System admin cannot change roles — 403")
    void updateRole_systemAdmin_forbidden() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SYSTEM_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user cannot update role — 403")
    void updateRole_regularUser_forbidden() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SYSTEM_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated update role — 401")
    void updateRole_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/2/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "SYSTEM_ADMIN"))))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/admin/libraries ─────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin gets paginated libraries — 200")
    void getLibraries_adminSuccess() throws Exception {
        Page<LibraryDTO> page = new PageImpl<>(
                List.of(LibraryDTO.builder().id(1L).name("City Library").isActive(true).build()),
                PageRequest.of(0, 12), 1);
        when(libraryService.getAllLibrariesForAdminPaged(isNull(), any())).thenReturn(page);

        mockMvc.perform(get("/v1/admin/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("City Library"));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin searches libraries — 200 with empty result")
    void getLibraries_withSearch_empty() throws Exception {
        Page<LibraryDTO> page = new PageImpl<>(List.of(), PageRequest.of(0, 12), 0);
        when(libraryService.getAllLibrariesForAdminPaged(eq("xyz"), any())).thenReturn(page);

        mockMvc.perform(get("/v1/admin/libraries").param("search", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user cannot get all libraries — 403")
    void getLibraries_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/v1/admin/libraries")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request to /admin/libraries — 401")
    void getLibraries_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/admin/libraries")).andExpect(status().isUnauthorized());
    }
}
