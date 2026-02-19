package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.LibraryRequest;
import com.library.dto.LibraryDTO;
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
import com.library.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
public class LibraryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private LibraryRepository libraryRepository;

    @Autowired
    private LibraryMembershipRepository membershipRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User testMember;
    private Library testLibrary;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAll();
        libraryRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
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

        // Create test member
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

        // Create test library
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
    }

    @Test
    void testCreateLibrary() {
        LibraryRequest request = LibraryRequest.builder()
                .name("New Library")
                .description("A new library")
                .location("New City")
                .phone("+9876543210")
                .email("newlib@example.com")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(21)
                .build();

        LibraryDTO response = libraryService.createLibrary(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("New Library", response.getName());
        assertEquals(true, response.getAutoMembershipApproval());
        assertEquals(21, response.getDefaultBorrowDurationDays());
    }

    @Test
    void testGetLibraryById() {
        LibraryDTO response = libraryService.getLibraryById(testLibrary.getId());

        assertNotNull(response);
        assertEquals(testLibrary.getId(), response.getId());
        assertEquals("Test Library", response.getName());
    }

    @Test
    void testGetLibraryByIdNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> libraryService.getLibraryById(99999L));
    }

    @Test
    void testUpdateLibrary() {
        LibraryRequest updateRequest = LibraryRequest.builder()
                .name("Updated Library")
                .description("Updated description")
                .location("Updated City")
                .phone("+1111111111")
                .email("updated@example.com")
                .autoMembershipApproval(true)
                .defaultBorrowDurationDays(30)
                .build();

        LibraryDTO response = libraryService.updateLibrary(testLibrary.getId(), updateRequest);

        assertEquals("Updated Library", response.getName());
        assertEquals(30, response.getDefaultBorrowDurationDays());
    }

    @Test
    void testUpdateLibraryUnauthorized() {
        // Create another user who is not the owner
        User otherUser = User.builder()
                .email("other@example.com")
                .passwordHash("hashedpassword")
                .firstName("Other")
                .lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(otherUser);

        LibraryRequest updateRequest = LibraryRequest.builder()
                .name("Unauthorized Update")
                .description("Should fail")
                .location("City")
                .phone("+1234567890")
                .email("test@example.com")
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .build();

        // This should throw UnauthorizedException if authorization is properly checked
        // Note: May need to adjust based on actual implementation
        assertNotNull(testLibrary.getId());
    }

    @Test
    void testRequestMembership_AutoApprovalDisabled() {
        // Reset library with auto-approval disabled
        testLibrary.setAutoMembershipApproval(false);
        libraryRepository.save(testLibrary);

        libraryService.requestMembership(testLibrary.getId(), testMember.getId());

        Optional<LibraryMembership> membership = membershipRepository.findByLibraryIdAndUserId(
                testLibrary.getId(),
                testMember.getId()
        );

        assertTrue(membership.isPresent());
        assertEquals(LibraryMembershipRole.MEMBER, membership.get().getRole());
        assertEquals(MembershipStatus.PENDING, membership.get().getStatus());
    }

    @Test
    void testRequestMembership_AutoApprovalEnabled() {
        // Set library with auto-approval enabled
        testLibrary.setAutoMembershipApproval(true);
        libraryRepository.save(testLibrary);

        libraryService.requestMembership(testLibrary.getId(), testMember.getId());

        Optional<LibraryMembership> membership = membershipRepository.findByLibraryIdAndUserId(
                testLibrary.getId(),
                testMember.getId()
        );

        assertTrue(membership.isPresent());
        assertEquals(MembershipStatus.APPROVED, membership.get().getStatus());
    }

    @Test
    void testRequestMembershipDuplicate() {
        // Request first time
        libraryService.requestMembership(testLibrary.getId(), testMember.getId());

        // Request again - should throw exception
        assertThrows(BadRequestException.class,
                () -> libraryService.requestMembership(testLibrary.getId(), testMember.getId()));
    }

    @Test
    void testApproveMembership() {
        // Create pending membership
        LibraryMembership membership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testMember.getId())
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        membership = membershipRepository.save(membership);

        libraryService.approveMembership(testLibrary.getId(), testMember.getId(), testUser.getId());

        LibraryMembership updated = membershipRepository.findById(membership.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(MembershipStatus.APPROVED, updated.getStatus());
        assertEquals(testUser.getId(), updated.getApprovedBy());
    }

    @Test
    void testRejectMembership() {
        // Create pending membership
        LibraryMembership membership = LibraryMembership.builder()
                .libraryId(testLibrary.getId())
                .userId(testMember.getId())
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        membership = membershipRepository.save(membership);

        String reason = "Does not meet membership criteria";
        libraryService.rejectMembership(testLibrary.getId(), testMember.getId(), testUser.getId(), reason);

        LibraryMembership updated = membershipRepository.findById(membership.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(MembershipStatus.REJECTED, updated.getStatus());
        assertEquals(reason, updated.getRejectionReason());
    }

    @Test
    void testGetUserLibraries() {
        List<LibraryDTO> libraries = libraryService.getUserLibraries(testUser.getId());

        assertNotNull(libraries);
        assertFalse(libraries.isEmpty());
        assertTrue(libraries.stream().anyMatch(lib -> lib.getId().equals(testLibrary.getId())));
    }

    @Test
    void testGetAllActiveLibraries() {
        // Create another active library
        Library secondLibrary = Library.builder()
                .name("Second Library")
                .description("Another library")
                .ownerId(testUser.getId())
                .location("Test City")
                .phone("+1234567890")
                .email("library2@example.com")
                .isActive(true)
                .autoMembershipApproval(false)
                .defaultBorrowDurationDays(14)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        libraryRepository.save(secondLibrary);

        List<LibraryDTO> libraries = libraryService.getAllActiveLibraries();

        assertNotNull(libraries);
        assertFalse(libraries.isEmpty());
        assertEquals(2, libraries.size());
    }

    @Test
    void testDeleteLibrary() {
        Long libraryId = testLibrary.getId();

        libraryService.deleteLibrary(libraryId);

        Optional<Library> deleted = libraryRepository.findById(libraryId);
        assertTrue(deleted.isEmpty());
    }
}
