package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.LibraryCreationRequestDTO;
import com.library.dto.LibraryRequest;
import com.library.entity.LibraryCreationRequest;
import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.LibraryRequestStatus;
import com.library.entity.enums.SystemRole;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.LibraryCreationRequestRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Library Request Service Tests")
class LibraryRequestServiceTest extends BaseIntegrationTest {

    @Autowired private LibraryRequestService libraryRequestService;
    @Autowired private LibraryCreationRequestRepository requestRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private UserRepository userRepository;

    private User regularUser;
    private User adminUser;
    private LibraryRequest validRequest;

    @BeforeEach
    void setUp() {
        regularUser = userRepository.save(User.builder()
                .email("req-user@lib.com").passwordHash("$2a$10$x")
                .firstName("R").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        adminUser = userRepository.save(User.builder()
                .email("req-admin@lib.com").passwordHash("$2a$10$x")
                .firstName("A").lastName("U").systemRole(SystemRole.SYSTEM_ADMIN)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        validRequest = LibraryRequest.builder()
                .name("کتابخانه شهر").description("کتابخانه عمومی شهر")
                .autoMembershipApproval(true).defaultBorrowDurationDays(14).build();
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    // ── createRequest ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Regular user submits a library request — PENDING")
    void createRequest_success() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryCreationRequestDTO result = libraryRequestService.createRequest(validRequest);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("کتابخانه شهر");
        assertThat(result.getStatus()).isEqualTo(LibraryRequestStatus.PENDING);
        assertThat(result.getRequesterEmail()).isEqualTo("req-user@lib.com");
    }

    @Test
    @DisplayName("Submit request with blank name throws BadRequestException")
    void createRequest_blankName_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryRequest bad = LibraryRequest.builder().name("   ").build();

        assertThatThrownBy(() -> libraryRequestService.createRequest(bad))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("نام کتابخانه");
    }

    // ── getMyRequests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getMyRequests returns only current user's requests")
    void getMyRequests_ownerOnly() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        libraryRequestService.createRequest(validRequest);

        // Create another request as admin to confirm isolation
        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        libraryRequestService.createRequest(LibraryRequest.builder().name("Admin Library").build());

        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        List<LibraryCreationRequestDTO> mine = libraryRequestService.getMyRequests();
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).getRequesterEmail()).isEqualTo("req-user@lib.com");
    }

    // ── getAllRequests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("System admin lists all requests — success")
    void getAllRequests_adminSuccess() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        List<LibraryCreationRequestDTO> all = libraryRequestService.getAllRequests(null);
        assertThat(all).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Regular user cannot list all requests — UnauthorizedException")
    void getAllRequests_regularUser_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        assertThatThrownBy(() -> libraryRequestService.getAllRequests(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("getAllRequests filtered by PENDING status")
    void getAllRequests_filteredByPending() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        List<LibraryCreationRequestDTO> pending = libraryRequestService.getAllRequests(LibraryRequestStatus.PENDING);
        assertThat(pending).allMatch(r -> r.getStatus() == LibraryRequestStatus.PENDING);
    }

    // ── approveRequest ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin approves a pending request — creates library and sets APPROVED")
    void approveRequest_success() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryCreationRequestDTO created = libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        LibraryCreationRequestDTO approved = libraryRequestService.approveRequest(created.getId(), null);

        assertThat(approved.getStatus()).isEqualTo(LibraryRequestStatus.APPROVED);
        assertThat(approved.getCreatedLibraryId()).isNotNull();
        assertThat(libraryRepository.findById(approved.getCreatedLibraryId())).isPresent();
    }

    @Test
    @DisplayName("Approving a non-PENDING request throws BadRequestException")
    void approveRequest_nonPending_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryCreationRequestDTO created = libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        libraryRequestService.approveRequest(created.getId(), null); // approve once

        assertThatThrownBy(() -> libraryRequestService.approveRequest(created.getId(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در انتظار");
    }

    @Test
    @DisplayName("Approving a non-existent request throws ResourceNotFoundException")
    void approveRequest_notFound_throws() {
        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        assertThatThrownBy(() -> libraryRequestService.approveRequest(999L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Regular user cannot approve requests")
    void approveRequest_regularUser_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        assertThatThrownBy(() -> libraryRequestService.approveRequest(1L, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ── rejectRequest ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Admin rejects a pending request with reason — REJECTED")
    void rejectRequest_success() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryCreationRequestDTO created = libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        LibraryCreationRequestDTO rejected = libraryRequestService.rejectRequest(created.getId(), "اطلاعات ناقص");

        assertThat(rejected.getStatus()).isEqualTo(LibraryRequestStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("اطلاعات ناقص");
    }

    @Test
    @DisplayName("Rejecting without reason throws BadRequestException")
    void rejectRequest_noReason_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryCreationRequestDTO created = libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        assertThatThrownBy(() -> libraryRequestService.rejectRequest(created.getId(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("دلیل");
    }

    @Test
    @DisplayName("Rejecting a non-PENDING request throws BadRequestException")
    void rejectRequest_nonPending_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        LibraryCreationRequestDTO created = libraryRequestService.createRequest(validRequest);

        SecurityTestUtils.setSecurityContext(adminUser, "SYSTEM_ADMIN");
        libraryRequestService.rejectRequest(created.getId(), "دلیل"); // reject once

        assertThatThrownBy(() -> libraryRequestService.rejectRequest(created.getId(), "دلیل دوم"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("در انتظار");
    }

    @Test
    @DisplayName("Regular user cannot reject requests")
    void rejectRequest_regularUser_throws() {
        SecurityTestUtils.setSecurityContext(regularUser, "USER");
        assertThatThrownBy(() -> libraryRequestService.rejectRequest(1L, "دلیل"))
                .isInstanceOf(UnauthorizedException.class);
    }
}
