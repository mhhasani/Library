package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.config.WithMockCustomUser;
import com.library.dto.LibraryDTO;
import com.library.dto.LibraryRequest;
import com.library.entity.Library;
import com.library.entity.LibraryMembership;
import com.library.entity.User;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.exception.ResourceNotFoundException;
import com.library.exception.UnauthorizedException;
import com.library.repository.LibraryMembershipRepository;
import com.library.repository.LibraryRepository;
import com.library.repository.UserRepository;
import com.library.util.SecurityTestUtils;
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
                .hasMessageContaining("Library not found");
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
                .hasMessageContaining("Only library admins");
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
                .hasMessageContaining("Only library owner can delete");
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
                .hasMessageContaining("already a member");
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
}
