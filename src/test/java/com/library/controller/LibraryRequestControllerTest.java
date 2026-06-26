package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LibraryCreationRequestDTO;
import com.library.dto.LibraryRequest;
import com.library.entity.enums.LibraryRequestStatus;
import com.library.service.LibraryRequestService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Library Request Controller Tests")
class LibraryRequestControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private LibraryRequestService service;

    private LibraryCreationRequestDTO pendingDTO;
    private LibraryCreationRequestDTO approvedDTO;

    @BeforeEach
    void setUp() {
        pendingDTO = LibraryCreationRequestDTO.builder()
                .id(1L).name("کتابخانه جدید").status(LibraryRequestStatus.PENDING).build();
        approvedDTO = LibraryCreationRequestDTO.builder()
                .id(1L).name("کتابخانه جدید").status(LibraryRequestStatus.APPROVED).createdLibraryId(10L).build();
    }

    // ── POST /v1/library-requests ─────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("Authenticated user submits library request — 201")
    void submit_authenticated_created() throws Exception {
        when(service.createRequest(any())).thenReturn(pendingDTO);

        mockMvc.perform(post("/v1/library-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LibraryRequest.builder().name("کتابخانه جدید").build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("Unauthenticated submit — 401")
    void submit_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(post("/v1/library-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LibraryRequest.builder().name("x").build())))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /v1/library-requests/mine ─────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("User lists own requests — 200")
    void mine_ok() throws Exception {
        when(service.getMyRequests()).thenReturn(List.of(pendingDTO));

        mockMvc.perform(get("/v1/library-requests/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Unauthenticated GET /mine — 401")
    void mine_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(get("/v1/library-requests/mine")).andExpect(status().isUnauthorized());
    }

    // ── GET /v1/library-requests (admin) ──────────────────────────────────

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin lists all requests — 200")
    void all_admin_ok() throws Exception {
        when(service.getAllRequests(isNull())).thenReturn(List.of(pendingDTO));

        mockMvc.perform(get("/v1/library-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin filters requests by PENDING status — 200")
    void all_admin_filtered() throws Exception {
        when(service.getAllRequests(LibraryRequestStatus.PENDING)).thenReturn(List.of(pendingDTO));

        mockMvc.perform(get("/v1/library-requests").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user cannot list all requests — 403")
    void all_regularUser_forbidden() throws Exception {
        mockMvc.perform(get("/v1/library-requests")).andExpect(status().isForbidden());
    }

    // ── POST /v1/library-requests/{id}/approve ────────────────────────────

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin approves a request — 200")
    void approve_admin_ok() throws Exception {
        when(service.approveRequest(eq(1L), isNull())).thenReturn(approvedDTO);

        mockMvc.perform(post("/v1/library-requests/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.createdLibraryId").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user cannot approve requests — 403")
    void approve_regularUser_forbidden() throws Exception {
        mockMvc.perform(post("/v1/library-requests/1/approve")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated approve — 401")
    void approve_unauthenticated_unauthorized() throws Exception {
        mockMvc.perform(post("/v1/library-requests/1/approve")).andExpect(status().isUnauthorized());
    }

    // ── POST /v1/library-requests/{id}/reject ─────────────────────────────

    @Test
    @WithMockUser(roles = "SYSTEM_ADMIN")
    @DisplayName("Admin rejects a request — 200")
    void reject_admin_ok() throws Exception {
        LibraryCreationRequestDTO rejected = LibraryCreationRequestDTO.builder()
                .id(1L).status(LibraryRequestStatus.REJECTED).rejectionReason("ناقص").build();
        when(service.rejectRequest(1L, "ناقص")).thenReturn(rejected);

        mockMvc.perform(post("/v1/library-requests/1/reject").param("reason", "ناقص"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Regular user cannot reject requests — 403")
    void reject_regularUser_forbidden() throws Exception {
        mockMvc.perform(post("/v1/library-requests/1/reject")).andExpect(status().isForbidden());
    }
}
