package com.library.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.BaseIntegrationTest;
import com.library.dto.LibraryRequest;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class LibraryControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    private User testUser;
    private User testMember;
    private Library testLibrary;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        libraryRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .email("admin@example.com")
                .passwordHash("hashedpassword")
                .firstName("Admin")
                .lastName("User")
                .systemRole(SystemRole.ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        testMember = User.builder()
                .email("member@example.com")
                .passwordHash("hashedpassword")
                .firstName("Member")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testMember = userRepository.save(testMember);

        testLibrary = Library.builder()
                .name("Test Library")
                .description("A test library")
                .ownerId(testUser.getId())
                .location("Test City")
                .phone("+1234567890")
                .email("library@example.com")
                .isActive(true)
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testLibrary = libraryRepository.save(testLibrary);

        LibraryMembership adminMembership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testUser.getId())
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .approvedAt(LocalDateTime.now())
                .approvedBy(testUser.getId())
                .build();
        membershipRepository.save(adminMembership);
    }

    @Test
    void testCreateLibrary() throws Exception {
        LibraryRequest request = LibraryRequest.builder()
                .name("New Library")
                .description("A new library")
                .location("New City")
                .phone("+9876543210")
                .email("newlib@example.com")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(21)
                .build();

        mockMvc.perform(post("/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", equalTo("New Library")))
                .andExpect(jsonPath("$.data.autoMembershipApproval", is(true)))
                .andExpect(jsonPath("$.data.defaultBorrowDurationDays", equalTo(21)));
    }

    @Test
    void testGetLibraryById() throws Exception {
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", equalTo(testLibrary.getId().intValue())))
                .andExpect(jsonPath("$.data.name", equalTo("Test Library")));
    }

    @Test
    void testGetLibraryNotFound() throws Exception {
        mockMvc.perform(get("/v1/libraries/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void testUpdateLibrary() throws Exception {
        LibraryRequest updateRequest = LibraryRequest.builder()
                .name("Updated Library")
                .description("Updated description")
                .location("Updated City")
                .phone("+1111111111")
                .email("updated@example.com")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(30)
                .build();

        mockMvc.perform(put("/v1/libraries/" + testLibrary.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", equalTo("Updated Library")))
                .andExpect(jsonPath("$.data.defaultBorrowDurationDays", equalTo(30)));
    }

    @Test
    void testDeleteLibrary() throws Exception {
        mockMvc.perform(delete("/v1/libraries/" + testLibrary.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify library is deleted
        mockMvc.perform(get("/v1/libraries/" + testLibrary.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetUserLibraries() throws Exception {
        mockMvc.perform(get("/v1/libraries/user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetAllActiveLibraries() throws Exception {
        mockMvc.perform(get("/v1/libraries")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testRequestMembership() throws Exception {
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/membership")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void testRequestMembershipDuplicate() throws Exception {
        // Request first time
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/membership")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Request again with mock user as different user
        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/membership")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setUserPrincipal(new org.springframework.security.core.userdetails.User(
                                    "member@example.com", "", 
                                    java.util.Collections.singleton(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
                            ));
                            return request;
                        }))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApproveMembership() throws Exception {
        // Create pending membership
        LibraryMembership membership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testMember.getId())
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        membership = membershipRepository.save(membership);

        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/membership/" + testMember.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify membership is approved
        LibraryMembership updated = membershipRepository.findById(membership.getId()).orElse(null);
        assert updated != null;
        assert updated.getStatus() == MembershipStatus.APPROVED;
    }

    @Test
    void testRejectMembership() throws Exception {
        // Create pending membership
        LibraryMembership membership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testMember.getId())
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        membership = membershipRepository.save(membership);

        mockMvc.perform(post("/v1/libraries/" + testLibrary.getId() + "/membership/" + testMember.getId() + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("reason", "Does not meet criteria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Verify membership is rejected
        LibraryMembership updated = membershipRepository.findById(membership.getId()).orElse(null);
        assert updated != null;
        assert updated.getStatus() == MembershipStatus.REJECTED;
    }
}
