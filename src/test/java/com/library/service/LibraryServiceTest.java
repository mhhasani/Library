package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.config.WithMockCustomUser;
import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.Notification;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.dto.MembershipDTO;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.NotificationRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Library Service Tests")
class LibraryServiceTest extends BaseIntegrationTest {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User ownerUser;
    private User memberUser;
    private Library library;
    private LibraryRequest libraryRequest;

    @BeforeEach
    void setUp() {
        // Create owner user
        ownerUser = new User();
        ownerUser.setEmail("owner@library.com");
        ownerUser.setPasswordHash("$2a$10$encoded");
        ownerUser.setFirstName("Owner");
        ownerUser.setLastName("User");
        ownerUser.setSystemRole(SystemRole.USER);
        ownerUser.setAccountStatus(AccountStatus.ACTIVE);
        ownerUser.setCreatedAt(LocalDateTime.now());
        ownerUser.setUpdatedAt(LocalDateTime.now());
        ownerUser = userRepository.save(ownerUser);

        // Create member user
        memberUser = new User();
        memberUser.setEmail("member@library.com");
        memberUser.setPasswordHash("$2a$10$encoded");
        memberUser.setFirstName("Member");
        memberUser.setLastName("User");
        memberUser.setSystemRole(SystemRole.USER);
        memberUser.setAccountStatus(AccountStatus.ACTIVE);
        memberUser.setCreatedAt(LocalDateTime.now());
        memberUser.setUpdatedAt(LocalDateTime.now());
        memberUser = userRepository.save(memberUser);

        // Create library
        library = Library.builder()
                .name("City Library")
                .description("Main city library")
                .owner(ownerUser)
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        library = libraryRepository.save(library);

        // Add owner as admin
        LibraryMembership ownerMembership = LibraryMembership.builder()
                .user(ownerUser)
                .library(library)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(ownerUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(ownerMembership);

        libraryRequest = LibraryRequest.builder()
                .name("New Library")
                .description("New library description")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(21)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    @Test
    @DisplayName("Should create library successfully")
    void testCreateLibrarySuccess() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        
        LibraryDTO result = libraryService.createLibrary(libraryRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("New Library");
        assertThat(result.getDescription()).isEqualTo("New library description");
        assertThat(result.getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should add owner as admin when creating library")
    void testCreateLibraryAddsOwnerAsAdmin() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        
        LibraryDTO result = libraryService.createLibrary(libraryRequest);

        Library createdLibrary = libraryRepository.findById(result.getId()).orElse(null);
        assertThat(createdLibrary).isNotNull();
        assertThat(createdLibrary.getOwner().getEmail()).isEqualTo("owner@library.com");

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(ownerUser.getId(), result.getId()).orElse(null);
        assertThat(membership).isNotNull();
        assertThat(membership.getRole()).isEqualTo(LibraryMembershipRole.ADMIN);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.APPROVED);
    }

    @Test
    @DisplayName("Should get library by id successfully")
    void testGetLibraryByIdSuccess() {
        LibraryDTO result = libraryService.getLibraryById(library.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(library.getId());
        assertThat(result.getName()).isEqualTo("City Library");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when library not found")
    void testGetLibraryNotFound() {
        assertThatThrownBy(() -> libraryService.getLibraryById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کتابخانه");
    }

    @Test
    @DisplayName("Should get all active libraries")
    void testGetAllActiveLibraries() {
        List<LibraryDTO> result = libraryService.getAllActiveLibraries();

        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(l -> l.getId().equals(library.getId()))).isTrue();
    }

    @Test
    @DisplayName("Should get user libraries")
    void testGetUserLibraries() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        
        List<LibraryDTO> result = libraryService.getUserLibraries();

        assertThat(result).isNotEmpty();
        assertThat(result.stream().anyMatch(l -> l.getId().equals(library.getId()))).isTrue();
    }

    @Test
    @DisplayName("Should update library successfully")
    void testUpdateLibrarySuccess() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        
        LibraryRequest updateRequest = LibraryRequest.builder()
                .name("Updated Library Name")
                .description("Updated description")
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(30)
                .build();

        LibraryDTO result = libraryService.updateLibrary(library.getId(), updateRequest);

        assertThat(result.getName()).isEqualTo("Updated Library Name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getDefaultBorrowDurationDays()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin tries to update")
    void testUpdateLibraryNonAdmin() {
        // Add member user as MEMBER
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        
        LibraryRequest updateRequest = LibraryRequest.builder()
                .name("Updated")
                .build();

        assertThatThrownBy(() -> libraryService.updateLibrary(library.getId(), updateRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مدیر کتابخانه");
    }

    @Test
    @DisplayName("Should delete library successfully")
    void testDeleteLibrarySuccess() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        
        libraryService.deleteLibrary(library.getId());

        Library deletedLibrary = libraryRepository.findById(library.getId()).orElse(null);
        assertThat(deletedLibrary).isNotNull();
        assertThat(deletedLibrary.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-owner tries to delete")
    void testDeleteLibraryNonOwner() {
        assertThatThrownBy(() -> libraryService.deleteLibrary(library.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مالک کتابخانه");
    }

    @Test
    @DisplayName("Should request membership successfully")
    void testRequestMembershipSuccess() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        
        libraryService.requestMembership(library.getId());

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(memberUser.getId(), library.getId()).orElse(null);
        assertThat(membership).isNotNull();
        assertThat(membership.getRole()).isEqualTo(LibraryMembershipRole.MEMBER);
        // Auto-approved since autoMembershipApproval is true
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.APPROVED);
    }

    @Test
    @DisplayName("Should throw BadRequestException when already a member")
    void testRequestMembershipAlreadyMember() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        
        // First request
        libraryService.requestMembership(library.getId());

        // Second request should fail
        assertThatThrownBy(() -> libraryService.requestMembership(library.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("از قبل عضو");
    }

    @Test
    @DisplayName("Should request membership as pending when auto-approval is disabled")
    void testRequestMembershipPending() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        
        // Create library with auto-approval disabled
        Library manualLibrary = Library.builder()
                .name("Manual Library")
                .owner(ownerUser)
                .autoMembershipApproval(false)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        manualLibrary = libraryRepository.save(manualLibrary);

        libraryService.requestMembership(manualLibrary.getId());

        LibraryMembership membership = membershipRepository.findByUserIdAndLibraryId(memberUser.getId(), manualLibrary.getId()).orElse(null);
        assertThat(membership).isNotNull();
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test
    @DisplayName("Should approve membership successfully")
    void testApproveMembershipSuccess() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        
        // Create pending membership
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membership = membershipRepository.save(membership);

        libraryService.approveMembership(library.getId(), memberUser.getId());

        LibraryMembership approvedMembership = membershipRepository.findById(membership.getId()).orElse(null);
        assertThat(approvedMembership).isNotNull();
        assertThat(approvedMembership.getStatus()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(approvedMembership.getApprovedBy().getId()).isEqualTo(ownerUser.getId());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin tries to approve")
    void testApproveMembershipNonAdmin() {
        assertThatThrownBy(() -> libraryService.approveMembership(library.getId(), memberUser.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---------- createLibrary: SYSTEM_ADMIN assigning a different owner ----------

    @Test
    @DisplayName("Should let SYSTEM_ADMIN create a library on behalf of another user")
    void testCreateLibrarySystemAdminAssignsOwner() {
        SecurityTestUtils.setSecurityContext(ownerUser, "SYSTEM_ADMIN");

        LibraryRequest request = LibraryRequest.builder()
                .name("Delegated Library")
                .description("desc")
                .ownerUserId(memberUser.getId())
                .build();

        LibraryDTO result = libraryService.createLibrary(request);

        assertThat(result.getOwnerId()).isEqualTo(memberUser.getId());
        LibraryMembership membership = membershipRepository
                .findByUserIdAndLibraryId(memberUser.getId(), result.getId()).orElse(null);
        assertThat(membership).isNotNull();
        assertThat(membership.getRole()).isEqualTo(LibraryMembershipRole.ADMIN);
    }

    @Test
    @DisplayName("Should ignore ownerUserId when caller is not SYSTEM_ADMIN")
    void testCreateLibraryNonSystemAdminIgnoresOwnerId() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        LibraryRequest request = LibraryRequest.builder()
                .name("Own Library")
                .ownerUserId(memberUser.getId())
                .build();

        LibraryDTO result = libraryService.createLibrary(request);

        assertThat(result.getOwnerId()).isEqualTo(ownerUser.getId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when owner does not exist")
    void testCreateLibraryOwnerNotFound() {
        SecurityTestUtils.setSecurityContext(ownerUser, "SYSTEM_ADMIN");

        LibraryRequest request = LibraryRequest.builder()
                .name("Orphan Library")
                .ownerUserId(999999L)
                .build();

        assertThatThrownBy(() -> libraryService.createLibrary(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("کاربر مالک");
    }

    @Test
    @DisplayName("Should apply default autoMembershipApproval and borrow duration when not provided")
    void testCreateLibraryDefaults() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        LibraryRequest request = LibraryRequest.builder()
                .name("Defaults Library")
                .build();

        LibraryDTO result = libraryService.createLibrary(request);

        assertThat(result.getAutoMembershipApproval()).isFalse();
        assertThat(result.getDefaultBorrowDurationDays()).isEqualTo(14);
    }

    // ---------- getLibraryById with membership present ----------

    @Test
    @DisplayName("Should include membership role/status when current user is a member")
    void testGetLibraryByIdWithMembership() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        LibraryDTO result = libraryService.getLibraryById(library.getId());

        assertThat(result.getUserRole()).isEqualTo(LibraryMembershipRole.ADMIN);
        assertThat(result.getUserStatus()).isEqualTo(MembershipStatus.APPROVED);
    }

    // ---------- getAllLibrariesForAdmin ----------

    @Test
    @DisplayName("Should get all libraries for admin regardless of active status")
    void testGetAllLibrariesForAdmin() {
        Library inactive = Library.builder()
                .name("Inactive Library")
                .owner(ownerUser)
                .autoMembershipApproval(false)
                .isActive(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        libraryRepository.save(inactive);

        List<LibraryDTO> result = libraryService.getAllLibrariesForAdmin();

        assertThat(result.stream().anyMatch(l -> l.getName().equals("Inactive Library"))).isTrue();
        assertThat(result.stream().anyMatch(l -> l.getId().equals(library.getId()))).isTrue();
    }

    // ---------- getAllLibrariesForAdminPaged (search) ----------

    @Test
    @DisplayName("Should return all libraries when search is blank")
    void testGetAllLibrariesForAdminPagedNoSearch() {
        Page<LibraryDTO> result = libraryService.getAllLibrariesForAdminPaged(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent().stream().anyMatch(l -> l.getId().equals(library.getId()))).isTrue();
    }

    @Test
    @DisplayName("Should filter libraries by name search")
    void testGetAllLibrariesForAdminPagedSearchByName() {
        Page<LibraryDTO> result = libraryService.getAllLibrariesForAdminPaged("city", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("City Library");
    }

    @Test
    @DisplayName("Should filter libraries by owner email search")
    void testGetAllLibrariesForAdminPagedSearchByOwnerEmail() {
        Page<LibraryDTO> result = libraryService.getAllLibrariesForAdminPaged("owner@library.com", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(library.getId());
    }

    @Test
    @DisplayName("Should return empty page when search matches nothing")
    void testGetAllLibrariesForAdminPagedSearchNoMatch() {
        Page<LibraryDTO> result = libraryService.getAllLibrariesForAdminPaged("nonexistentxyz", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ---------- updateLibrary: SYSTEM_ADMIN branch + isActive branch ----------

    @Test
    @DisplayName("Should let SYSTEM_ADMIN update a library they don't belong to")
    void testUpdateLibrarySystemAdmin() {
        SecurityTestUtils.setSecurityContext(memberUser, "SYSTEM_ADMIN");

        LibraryRequest updateRequest = LibraryRequest.builder()
                .name("Admin Updated")
                .isActive(false)
                .build();

        LibraryDTO result = libraryService.updateLibrary(library.getId(), updateRequest);

        assertThat(result.getName()).isEqualTo("Admin Updated");
        assertThat(result.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a non-existent library")
    void testUpdateLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        assertThatThrownBy(() -> libraryService.updateLibrary(999L, libraryRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- deleteLibrary: SYSTEM_ADMIN branch ----------

    @Test
    @DisplayName("Should let SYSTEM_ADMIN delete a library they don't own")
    void testDeleteLibrarySystemAdmin() {
        SecurityTestUtils.setSecurityContext(memberUser, "SYSTEM_ADMIN");

        libraryService.deleteLibrary(library.getId());

        Library deleted = libraryRepository.findById(library.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting a non-existent library")
    void testDeleteLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        assertThatThrownBy(() -> libraryService.deleteLibrary(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- requestMembership: not-found branches ----------

    @Test
    @DisplayName("Should throw ResourceNotFoundException when requesting membership for a non-existent library")
    void testRequestMembershipLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        assertThatThrownBy(() -> libraryService.requestMembership(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when requesting membership as an unpersisted user")
    void testRequestMembershipCurrentUserNotFound() {
        User phantom = User.builder()
                .id(9999999L)
                .email("phantom@library.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Phantom")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        SecurityTestUtils.setSecurityContext(phantom, "USER");

        assertThatThrownBy(() -> libraryService.requestMembership(library.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("کاربر فعلی پیدا نشد");
    }

    // ---------- setMemberRole ----------

    @Test
    @DisplayName("Should promote a member to ADMIN")
    void testSetMemberRolePromote() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        libraryService.setMemberRole(library.getId(), memberUser.getId(), LibraryMembershipRole.ADMIN);

        LibraryMembership updated = membershipRepository
                .findByUserIdAndLibraryId(memberUser.getId(), library.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(LibraryMembershipRole.ADMIN);
    }

    @Test
    @DisplayName("Should let SYSTEM_ADMIN change member roles")
    void testSetMemberRoleAsSystemAdmin() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(memberUser, "SYSTEM_ADMIN");
        libraryService.setMemberRole(library.getId(), memberUser.getId(), LibraryMembershipRole.ADMIN);

        LibraryMembership updated = membershipRepository
                .findByUserIdAndLibraryId(memberUser.getId(), library.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(LibraryMembershipRole.ADMIN);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-owner non-system-admin sets member role")
    void testSetMemberRoleUnauthorized() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> libraryService.setMemberRole(library.getId(), memberUser.getId(), LibraryMembershipRole.ADMIN))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مالک کتابخانه");
    }

    @Test
    @DisplayName("Should throw BadRequestException when trying to change the owner's role")
    void testSetMemberRoleOwnerCannotBeChanged() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        assertThatThrownBy(() -> libraryService.setMemberRole(library.getId(), ownerUser.getId(), LibraryMembershipRole.MEMBER))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("نقش مالک کتابخانه قابل تغییر نیست");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when target user is not a member")
    void testSetMemberRoleTargetNotMember() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        assertThatThrownBy(() -> libraryService.setMemberRole(library.getId(), memberUser.getId(), LibraryMembershipRole.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("عضو این کتابخانه نیست");
    }

    @Test
    @DisplayName("Should throw BadRequestException when target membership is not APPROVED")
    void testSetMemberRolePendingMember() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        assertThatThrownBy(() -> libraryService.setMemberRole(library.getId(), memberUser.getId(), LibraryMembershipRole.ADMIN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("تأییدشده");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when setting role on a non-existent library")
    void testSetMemberRoleLibraryNotFound() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        assertThatThrownBy(() -> libraryService.setMemberRole(999L, memberUser.getId(), LibraryMembershipRole.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- rejectMembership ----------

    @Test
    @DisplayName("Should reject membership successfully")
    void testRejectMembershipSuccess() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membership = membershipRepository.save(membership);

        libraryService.rejectMembership(library.getId(), memberUser.getId(), "Not eligible");

        LibraryMembership rejected = membershipRepository.findById(membership.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(MembershipStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Not eligible");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member tries to reject")
    void testRejectMembershipNonMember() {
        assertThatThrownBy(() -> libraryService.rejectMembership(library.getId(), memberUser.getId(), "reason"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("عضو این کتابخانه نیستید");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin member tries to reject")
    void testRejectMembershipNonAdminMember() {
        LibraryMembership adminlessMembership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(adminlessMembership);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> libraryService.rejectMembership(library.getId(), memberUser.getId(), "reason"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مدیر کتابخانه");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when rejecting a non-existent membership")
    void testRejectMembershipNotFound() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");

        User outsider = User.builder()
                .email("outsider-reject@library.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Out").lastName("Sider")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        outsider = userRepository.save(outsider);

        Long outsiderId = outsider.getId();
        assertThatThrownBy(() -> libraryService.rejectMembership(library.getId(), outsiderId, "reason"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- getLibraryMembers ----------

    @Test
    @DisplayName("Should get library members as admin")
    void testGetLibraryMembersSuccess() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        List<MembershipDTO> members = libraryService.getLibraryMembers(library.getId());

        assertThat(members).hasSizeGreaterThanOrEqualTo(2);
        assertThat(members.stream().anyMatch(m -> m.getUserId().equals(memberUser.getId()))).isTrue();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member fetches library members")
    void testGetLibraryMembersNonMember() {
        assertThatThrownBy(() -> libraryService.getLibraryMembers(library.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("عضو این کتابخانه نیستید");
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin member fetches library members")
    void testGetLibraryMembersNonAdminMember() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> libraryService.getLibraryMembers(library.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("فقط مدیر کتابخانه");
    }

    // ---------- getPendingMembers / requireLibraryAdmin ----------

    @Test
    @DisplayName("Should get pending members as library admin")
    void testGetPendingMembersSuccess() {
        LibraryMembership pending = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(pending);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        List<MembershipDTO> result = libraryService.getPendingMembers(library.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(memberUser.getId());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member fetches pending members")
    void testGetPendingMembersNonMember() {
        assertThatThrownBy(() -> libraryService.getPendingMembers(library.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should allow SYSTEM_ADMIN member with non-admin role to fetch pending members")
    void testGetPendingMembersSystemAdminNonLibraryAdmin() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);
        SecurityTestUtils.setSecurityContext(memberUser, "SYSTEM_ADMIN");

        List<MembershipDTO> result = libraryService.getPendingMembers(library.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin non-system-admin member fetches pending members")
    void testGetPendingMembersNonAdminMember() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        assertThatThrownBy(() -> libraryService.getPendingMembers(library.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---------- getMembersPaged (search) ----------

    @Test
    @DisplayName("Should return non-pending members without search filter")
    void testGetMembersPagedNoSearch() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        Page<MembershipDTO> result = libraryService.getMembersPaged(library.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2); // owner + member, excludes any pending
    }

    @Test
    @DisplayName("Should filter members by search term")
    void testGetMembersPagedWithSearch() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        Page<MembershipDTO> result = libraryService.getMembersPaged(library.getId(), "member@library.com", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(memberUser.getId());
    }

    @Test
    @DisplayName("Should exclude pending members from the paged member listing")
    void testGetMembersPagedExcludesPending() {
        LibraryMembership pending = LibraryMembership.builder()
                .user(memberUser)
                .library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(pending);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        Page<MembershipDTO> result = libraryService.getMembersPaged(library.getId(), null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1); // only the owner
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(ownerUser.getId());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-admin fetches paged members")
    void testGetMembersPagedUnauthorized() {
        assertThatThrownBy(() -> libraryService.getMembersPaged(library.getId(), null, PageRequest.of(0, 10)))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---------- notifications ----------

    @Test
    @DisplayName("requestMembership with auto-approval notifies the user directly (MEMBERSHIP_APPROVED)")
    void testRequestMembership_autoApproved_notifiesUser() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        libraryService.requestMembership(library.getId()); // library has autoMembershipApproval = true

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(memberUser.getId());
        assertThat(notifs).anyMatch(n -> n.getType() == NotificationType.MEMBERSHIP_APPROVED);
    }

    @Test
    @DisplayName("requestMembership without auto-approval notifies library admins (NEW_MEMBERSHIP_REQUEST)")
    void testRequestMembership_pending_notifiesAdmins() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        Library manualLibrary = Library.builder()
                .name("Manual Library").owner(ownerUser)
                .autoMembershipApproval(false).isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        manualLibrary = libraryRepository.save(manualLibrary);
        membershipRepository.save(LibraryMembership.builder()
                .user(ownerUser).library(manualLibrary)
                .role(LibraryMembershipRole.ADMIN).status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        libraryService.requestMembership(manualLibrary.getId());

        List<Notification> ownerNotifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(ownerUser.getId());
        assertThat(ownerNotifs).anyMatch(n -> n.getType() == NotificationType.NEW_MEMBERSHIP_REQUEST);
    }

    @Test
    @DisplayName("approveMembership notifies the user")
    void testApproveMembership_notifiesUser() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser).library(library)
                .role(LibraryMembershipRole.MEMBER).status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        libraryService.approveMembership(library.getId(), memberUser.getId());

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(memberUser.getId());
        assertThat(notifs).anyMatch(n -> n.getType() == NotificationType.MEMBERSHIP_APPROVED);
    }

    @Test
    @DisplayName("rejectMembership notifies the user with the rejection reason")
    void testRejectMembership_notifiesUser() {
        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser).library(library)
                .role(LibraryMembershipRole.MEMBER).status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        libraryService.rejectMembership(library.getId(), memberUser.getId(), "Not eligible");

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(memberUser.getId());
        Notification n = notifs.stream().filter(x -> x.getType() == NotificationType.MEMBERSHIP_REJECTED)
                .findFirst().orElseThrow();
        assertThat(n.getMessage()).contains("Not eligible");
    }

    @Test
    @DisplayName("setMemberRole notifies the affected user")
    void testSetMemberRole_notifiesUser() {
        LibraryMembership membership = LibraryMembership.builder()
                .user(memberUser).library(library)
                .role(LibraryMembershipRole.MEMBER).status(MembershipStatus.APPROVED)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        membershipRepository.save(membership);

        SecurityTestUtils.setSecurityContext(ownerUser, "USER");
        libraryService.setMemberRole(library.getId(), memberUser.getId(), LibraryMembershipRole.ADMIN);

        List<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(memberUser.getId());
        assertThat(notifs).anyMatch(n -> n.getType() == NotificationType.LIBRARY_ROLE_CHANGED);
    }
}
