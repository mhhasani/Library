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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin User Controller Tests")
class AdminUserControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private LibraryService libraryService;

    private UserDTO activeUser;
    private UserDTO suspendedUser;

    @BeforeEach
    void setUp() {
        activeUser = UserDTO.builder()
                .id(2L)
                .email("user@test.com")
                .firstName("Ali")
                .lastName("Ahmadi")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        suspendedUser = UserDTO.builder()
                .id(2L)
                .email("user@test.com")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.SUSPENDED)
                .build();
    }

    // ── GET /v1/admin/users ───────────────────────

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should get all users successfully")
    void testGetUsersSuccess() throws Exception {
        when(userService.getUsers(null)).thenReturn(Arrays.asList(activeUser));

        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("user@test.com"))
                .andExpect(jsonPath("$.data[0].accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should get users filtered by status")
    void testGetUsersFilteredByStatus() throws Exception {
        when(userService.getUsers(AccountStatus.ACTIVE)).thenReturn(Arrays.asList(activeUser));

        mockMvc.perform(get("/v1/admin/users").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 403 when regular user tries to get users")
    void testGetUsersForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated")
    void testGetUsersUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // ── PATCH /v1/admin/users/{id}/status ────────

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should suspend user successfully")
    void testUpdateUserStatusSuspendSuccess() throws Exception {
        when(userService.updateUserStatus(2L, AccountStatus.SUSPENDED)).thenReturn(suspendedUser);

        mockMvc.perform(patch("/v1/admin/users/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountStatus").value("SUSPENDED"));
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should activate user successfully")
    void testUpdateUserStatusActivateSuccess() throws Exception {
        when(userService.updateUserStatus(2L, AccountStatus.ACTIVE)).thenReturn(activeUser);

        mockMvc.perform(patch("/v1/admin/users/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountStatus").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should return 400 when trying to change own status")
    void testUpdateUserStatusSelfChangeNotAllowed() throws Exception {
        doThrow(new BadRequestException("Cannot change your own account status"))
                .when(userService).updateUserStatus(2L, AccountStatus.SUSPENDED);

        mockMvc.perform(patch("/v1/admin/users/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should return 404 when user not found for status update")
    void testUpdateUserStatusNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("User not found with id: 999"))
                .when(userService).updateUserStatus(999L, AccountStatus.SUSPENDED);

        mockMvc.perform(patch("/v1/admin/users/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 403 when regular user tries to update status")
    void testUpdateUserStatusForbiddenForRegularUser() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/2/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /v1/admin/users/{id}/role ──────────

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should promote user to system admin successfully")
    void testUpdateUserRolePromoteSuccess() throws Exception {
        UserDTO promotedUser = UserDTO.builder()
                .id(2L)
                .email("user@test.com")
                .systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(userService.updateUserRole(2L, SystemRole.SYSTEM_ADMIN)).thenReturn(promotedUser);

        mockMvc.perform(patch("/v1/admin/users/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "SYSTEM_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.systemRole").value("SYSTEM_ADMIN"));
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should demote system admin to regular user")
    void testUpdateUserRoleDemoteSuccess() throws Exception {
        when(userService.updateUserRole(2L, SystemRole.USER)).thenReturn(activeUser);

        mockMvc.perform(patch("/v1/admin/users/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.systemRole").value("USER"));
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should return 400 when trying to change own role")
    void testUpdateUserRoleSelfChangeNotAllowed() throws Exception {
        doThrow(new BadRequestException("Cannot change your own system role"))
                .when(userService).updateUserRole(2L, SystemRole.USER);

        mockMvc.perform(patch("/v1/admin/users/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "USER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 403 when regular user tries to update role")
    void testUpdateUserRoleForbiddenForRegularUser() throws Exception {
        mockMvc.perform(patch("/v1/admin/users/2/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "SYSTEM_ADMIN"))))
                .andExpect(status().isForbidden());
    }

    // ── GET /v1/admin/libraries ───────────────────

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should get all libraries successfully")
    void testGetAllLibrariesSuccess() throws Exception {
        List<LibraryDTO> libraries = Arrays.asList(
                LibraryDTO.builder().id(1L).name("City Library").isActive(true).build(),
                LibraryDTO.builder().id(2L).name("Science Library").isActive(false).build()
        );

        when(libraryService.getAllLibrariesForAdmin()).thenReturn(libraries);

        mockMvc.perform(get("/v1/admin/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("City Library"))
                .andExpect(jsonPath("$.data[1].name").value("Science Library"));
    }

    @Test
    @WithMockUser(username = "sysadmin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should return empty list when no libraries exist")
    void testGetAllLibrariesEmpty() throws Exception {
        when(libraryService.getAllLibrariesForAdmin()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/v1/admin/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 403 when regular user tries to get all libraries")
    void testGetAllLibrariesForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/v1/admin/libraries"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated requests all libraries")
    void testGetAllLibrariesUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/admin/libraries"))
                .andExpect(status().isUnauthorized());
    }
}
