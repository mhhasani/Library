package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.dto.MembershipDTO;
import com.library.entity.enums.LibraryMembershipRole;
import com.library.entity.enums.MembershipStatus;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.service.LibraryService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Library Controller Tests")
class LibraryControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryService libraryService;

    private LibraryRequest libraryRequest;
    private LibraryDTO libraryDTO;

    @BeforeEach
    void setUp() {
        libraryRequest = LibraryRequest.builder()
                .name("City Library")
                .description("Main city library")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .build();

        libraryDTO = LibraryDTO.builder()
                .id(1L)
                .name("City Library")
                .description("Main city library")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .build();
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should create library successfully")
    void testCreateLibrarySuccess() throws Exception {
        when(libraryService.createLibrary(any(LibraryRequest.class))).thenReturn(libraryDTO);

        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Library created successfully"))
                .andExpect(jsonPath("$.data.name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 403 when regular user tries to create library")
    void testCreateLibraryForbiddenForRegularUser() throws Exception {
        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "SYSTEM_ADMIN")
    @DisplayName("Should return 400 when library name is blank")
    void testCreateLibraryInvalidRequest() throws Exception {
        LibraryRequest invalidRequest = LibraryRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get library by id successfully")
    void testGetLibrarySuccess() throws Exception {
        when(libraryService.getLibraryById(1L)).thenReturn(libraryDTO);

        mockMvc.perform(get("/v1/libraries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when library not found")
    void testGetLibraryNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Library not found"))
                .when(libraryService).getLibraryById(999L);

        mockMvc.perform(get("/v1/libraries/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get user libraries successfully")
    void testGetUserLibrariesSuccess() throws Exception {
        List<LibraryDTO> libraries = Arrays.asList(libraryDTO);
        when(libraryService.getUserLibraries()).thenReturn(libraries);

        mockMvc.perform(get("/v1/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should get user libraries empty list")
    void testGetUserLibrariesEmpty() throws Exception {
        when(libraryService.getUserLibraries()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/v1/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Should get active libraries publicly")
    void testGetActiveLibrariesPublic() throws Exception {
        List<LibraryDTO> libraries = Arrays.asList(libraryDTO);
        when(libraryService.getAllActiveLibraries()).thenReturn(libraries);

        mockMvc.perform(get("/v1/libraries/public/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should update library successfully")
    void testUpdateLibrarySuccess() throws Exception {
        LibraryDTO updatedLibrary = LibraryDTO.builder()
                .id(1L)
                .name("City Library Updated")
                .description("Updated description")
                .build();

        when(libraryService.updateLibrary(1L, libraryRequest)).thenReturn(updatedLibrary);

        mockMvc.perform(put("/v1/libraries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("City Library Updated"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when user not authorized to update")
    void testUpdateLibraryUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can update library"))
                .when(libraryService).updateLibrary(1L, libraryRequest);

        mockMvc.perform(put("/v1/libraries/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void testCreateLibraryUnAuthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(libraryRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get library members successfully")
    void testGetLibraryMembersSuccess() throws Exception {
        MembershipDTO member = MembershipDTO.builder()
                .id(1L)
                .userId(2L)
                .userEmail("member@test.com")
                .userName("Ali Ahmadi")
                .libraryId(1L)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .build();

        when(libraryService.getLibraryMembers(1L)).thenReturn(Arrays.asList(member));

        mockMvc.perform(get("/v1/libraries/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userEmail").value("member@test.com"))
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin requests member list")
    void testGetLibraryMembersUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can view member list"))
                .when(libraryService).getLibraryMembers(1L);

        mockMvc.perform(get("/v1/libraries/1/members"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user requests member list")
    void testGetLibraryMembersUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/members"))
                .andExpect(status().isUnauthorized());
    }

    // ── delete library ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner@library.com", roles = "USER")
    @DisplayName("Should delete library successfully")
    void testDeleteLibrarySuccess() throws Exception {
        mockMvc.perform(delete("/v1/libraries/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Library deleted successfully"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when deleting a nonexistent library")
    void testDeleteLibraryNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Library not found"))
                .when(libraryService).deleteLibrary(999L);

        mockMvc.perform(delete("/v1/libraries/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-owner deletes library")
    void testDeleteLibraryUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only the owner can delete this library"))
                .when(libraryService).deleteLibrary(1L);

        mockMvc.perform(delete("/v1/libraries/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user deletes library")
    void testDeleteLibraryUnauthenticated() throws Exception {
        mockMvc.perform(delete("/v1/libraries/1"))
                .andExpect(status().isUnauthorized());
    }

    // ── pending members ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should get pending members successfully")
    void testGetPendingMembersSuccess() throws Exception {
        MembershipDTO pending = MembershipDTO.builder()
                .id(2L).userId(3L).userEmail("pending@test.com")
                .libraryId(1L).role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .build();
        when(libraryService.getPendingMembers(1L)).thenReturn(Arrays.asList(pending));

        mockMvc.perform(get("/v1/libraries/1/members/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userEmail").value("pending@test.com"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin requests pending members")
    void testGetPendingMembersUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can view pending members"))
                .when(libraryService).getPendingMembers(1L);

        mockMvc.perform(get("/v1/libraries/1/members/pending"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user requests pending members")
    void testGetPendingMembersUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/members/pending"))
                .andExpect(status().isUnauthorized());
    }

    // ── search / paginate members ───────────────────────────────────────

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should search library members with pagination")
    void testSearchMembersSuccess() throws Exception {
        MembershipDTO member = MembershipDTO.builder()
                .id(1L).userId(2L).userEmail("member@test.com")
                .libraryId(1L).role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .build();
        org.springframework.data.domain.Page<MembershipDTO> page =
                new org.springframework.data.domain.PageImpl<>(Arrays.asList(member),
                        org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(libraryService.getMembersPaged(eq(1L), eq("ali"), any())).thenReturn(page);

        mockMvc.perform(get("/v1/libraries/1/members/search")
                .param("search", "ali")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userEmail").value("member@test.com"));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should search library members without a search term")
    void testSearchMembersNoQuery() throws Exception {
        org.springframework.data.domain.Page<MembershipDTO> emptyPage =
                new org.springframework.data.domain.PageImpl<>(Arrays.asList(),
                        org.springframework.data.domain.PageRequest.of(0, 10), 0);
        when(libraryService.getMembersPaged(eq(1L), isNull(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/v1/libraries/1/members/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user searches members")
    void testSearchMembersUnauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/1/members/search"))
                .andExpect(status().isUnauthorized());
    }

    // ── set member role ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner@library.com", roles = "USER")
    @DisplayName("Should set member role successfully")
    void testSetMemberRoleSuccess() throws Exception {
        when(libraryService.setMemberRole(1L, 2L, LibraryMembershipRole.ADMIN)).thenReturn(libraryDTO);

        mockMvc.perform(patch("/v1/libraries/1/members/2/role")
                .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("City Library"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-owner sets member role")
    void testSetMemberRoleUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only the owner can change roles"))
                .when(libraryService).setMemberRole(1L, 2L, LibraryMembershipRole.ADMIN);

        mockMvc.perform(patch("/v1/libraries/1/members/2/role")
                .param("role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when setting role for nonexistent membership")
    void testSetMemberRoleNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Membership not found"))
                .when(libraryService).setMemberRole(1L, 999L, LibraryMembershipRole.ADMIN);

        mockMvc.perform(patch("/v1/libraries/1/members/999/role")
                .param("role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user sets member role")
    void testSetMemberRoleUnauthenticated() throws Exception {
        mockMvc.perform(patch("/v1/libraries/1/members/2/role")
                .param("role", "ADMIN"))
                .andExpect(status().isUnauthorized());
    }

    // ── membership request/approve/reject ──────────────────────────────

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should request membership successfully")
    void testRequestMembershipSuccess() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/request"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Membership request submitted successfully"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 404 when requesting membership for nonexistent library")
    void testRequestMembershipNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Library not found"))
                .when(libraryService).requestMembership(999L);

        mockMvc.perform(post("/v1/libraries/999/membership/request"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user requests membership")
    void testRequestMembershipUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/request"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should approve membership successfully")
    void testApproveMembershipSuccess() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/2/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Membership approved successfully"));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin approves membership")
    void testApproveMembershipUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can approve membership"))
                .when(libraryService).approveMembership(1L, 2L);

        mockMvc.perform(post("/v1/libraries/1/membership/2/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user approves membership")
    void testApproveMembershipUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/2/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should reject membership successfully with reason")
    void testRejectMembershipSuccess() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/2/reject")
                .param("reason", "Not eligible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Membership rejected successfully"));
    }

    @Test
    @WithMockUser(username = "admin@library.com", roles = "USER")
    @DisplayName("Should reject membership successfully without reason")
    void testRejectMembershipNoReason() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/2/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "user@library.com", roles = "USER")
    @DisplayName("Should return 401 when non-admin rejects membership")
    void testRejectMembershipUnauthorized() throws Exception {
        doThrow(new UnauthorizedException("Only library admins can reject membership"))
                .when(libraryService).rejectMembership(1L, 2L, null);

        mockMvc.perform(post("/v1/libraries/1/membership/2/reject"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when unauthenticated user rejects membership")
    void testRejectMembershipUnauthenticated() throws Exception {
        mockMvc.perform(post("/v1/libraries/1/membership/2/reject"))
                .andExpect(status().isUnauthorized());
    }
}
