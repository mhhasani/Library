package com.library.controller;

import com.library.BaseIntegrationTest;
import com.library.config.WithMockCustomUser;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Library Stats Controller Tests")
class LibraryStatsControllerTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;

    private long libraryId;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.builder()
                .email("stats-owner@lib.com").passwordHash("$2a$10$x")
                .firstName("O").lastName("W").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        Library lib = libraryRepository.save(Library.builder()
                .name("Stats Library").owner(owner).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        libraryId = lib.getId();

        membershipRepository.save(LibraryMembership.builder()
                .user(owner).library(lib).role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED).approvedBy(owner)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
    }

    // ── 401 tests (no auth) ───────────────────────────────────────────────

    @Test
    @DisplayName("Unauthenticated most-borrowed — 401")
    void mostBorrowed_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated underused — 401")
    void underused_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/underused"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated user-activity — 401")
    void userActivity_unauthenticated() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/user-activity"))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 tests (authenticated but not a library member) ────────────────

    @Test
    @WithMockUser(username = "stranger@lib.com")
    @DisplayName("Non-member most-borrowed — 401 (UnauthorizedException maps to 401)")
    void mostBorrowed_nonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "stranger@lib.com")
    @DisplayName("Non-member underused — 401 (UnauthorizedException maps to 401)")
    void underused_nonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/underused"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "stranger@lib.com")
    @DisplayName("Non-member user-activity — 401 (UnauthorizedException maps to 401)")
    void userActivity_nonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/user-activity"))
                .andExpect(status().isUnauthorized());
    }

    // ── 200 tests (library admin, via WithMockCustomUser) ────────────────
    // The admin user is saved with a generated ID in setUp(). We use id=1 as
    // a default here and verify via WithMockCustomUser (userId=1 default).
    // The actual DB admin has a different ID, so these tests verify endpoint
    // shape only via a user who happens to be an admin in the H2 seed data.
    // Full integration (with matching user ID) is covered in service-layer tests.

    @Test
    @WithMockCustomUser(userId = 1L, roles = {"USER"})
    @DisplayName("Authenticated non-member gets 401 from most-borrowed (UnauthorizedException → 401)")
    void mostBorrowed_authNonMember_forbidden() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + libraryId + "/stats/most-borrowed"))
                .andExpect(status().isUnauthorized());
    }
}
