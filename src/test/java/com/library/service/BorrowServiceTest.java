package com.library.service;

import com.library.BaseIntegrationTest;
import com.library.dto.BorrowDTO;
import com.library.dto.BorrowRequest;
import com.library.entity.*;
import com.library.entity.enums.*;
import com.library.exception.BadRequestException;
import com.library.exception.UnauthorizedException;
import com.library.repository.*;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Borrow Service Tests")
class BorrowServiceTest extends BaseIntegrationTest {

    @Autowired private BorrowService borrowService;
    @Autowired private BorrowRepository borrowRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private BookCopyRepository bookCopyRepository;
    @Autowired private DigitalBookRepository digitalBookRepository;
    @Autowired private FileResourceRepository fileResourceRepository;
    @Autowired private LibraryRepository libraryRepository;
    @Autowired private LibraryMembershipRepository membershipRepository;
    @Autowired private UserRepository userRepository;

    private User adminUser;
    private User memberUser;
    private Library library;
    private Book book;
    private BookCopy bookCopy;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("admin@borrow.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Admin").lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        memberUser = userRepository.save(User.builder()
                .email("member@borrow.com")
                .passwordHash("$2a$10$encoded")
                .firstName("Member").lastName("User")
                .systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        library = libraryRepository.save(Library.builder()
                .name("Borrow Test Library")
                .owner(adminUser)
                .defaultBorrowDurationDays(14)
                .isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        membershipRepository.save(LibraryMembership.builder()
                .user(adminUser).library(library)
                .role(LibraryMembershipRole.ADMIN)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        membershipRepository.save(LibraryMembership.builder()
                .user(memberUser).library(library)
                .role(LibraryMembershipRole.MEMBER)
                .status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        book = bookRepository.save(Book.builder()
                .library(library)
                .title("Test Book")
                .author("Test Author")
                .autoDigitalBorrowEnabled(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());

        bookCopy = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(library)
                .copyNumber(1)
                .status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearSecurityContext();
    }

    // ── Physical Borrow ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create physical borrow request successfully")
    void createPhysicalBorrow_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build();

        BorrowDTO result = borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThat(result.getBorrowType()).isEqualTo(BorrowType.PHYSICAL);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.REQUESTED);
        assertThat(result.getBookId()).isEqualTo(book.getId());
        assertThat(result.getBookCopyId()).isNotNull();
    }

    @Test
    @DisplayName("Should throw BadRequestException when no copy available")
    void createPhysicalBorrow_noCopyAvailable_throws() {
        bookCopy.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(bookCopy);
        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No available copy");
    }

    @Test
    @DisplayName("Should throw BadRequestException when user already has active physical borrow")
    void createPhysicalBorrow_duplicate_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build();

        borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has an active physical borrow");
    }

    // ── Digital Borrow ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create digital borrow with REQUESTED status when auto-approve is off")
    void createDigitalBorrow_manualApproval() {
        FileResource fr = fileResourceRepository.save(FileResource.builder()
                .originalFilename("book.pdf").storedFilename("uuid.pdf")
                .filePath("digital-books/uuid.pdf")
                .fileSizeBytes(1024L).contentType("application/pdf")
                .checksumSha256("abc123").uploadedBy(adminUser)
                .createdAt(LocalDateTime.now()).build());
        digitalBookRepository.save(DigitalBook.builder()
                .book(book).fileResource(fr).fileFormat("PDF")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        BorrowDTO result = borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThat(result.getBorrowType()).isEqualTo(BorrowType.DIGITAL);
        assertThat(result.getStatus()).isEqualTo(BorrowStatus.REQUESTED);
    }

    @Test
    @DisplayName("Should auto-approve digital borrow when autoDigitalBorrowEnabled is true")
    void createDigitalBorrow_autoApproved() {
        book.setAutoDigitalBorrowEnabled(true);
        bookRepository.save(book);

        FileResource fr = fileResourceRepository.save(FileResource.builder()
                .originalFilename("book.pdf").storedFilename("uuid.pdf")
                .filePath("digital-books/uuid.pdf")
                .fileSizeBytes(1024L).contentType("application/pdf")
                .checksumSha256("abc456").uploadedBy(adminUser)
                .createdAt(LocalDateTime.now()).build());
        digitalBookRepository.save(DigitalBook.builder()
                .book(book).fileResource(fr).fileFormat("PDF")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        BorrowDTO result = borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThat(result.getStatus()).isEqualTo(BorrowStatus.APPROVED);
        assertThat(result.getBorrowDate()).isNotNull();
        assertThat(result.getDueDate()).isNotNull();
    }

    @Test
    @DisplayName("Should throw BadRequestException when book has no digital versions")
    void createDigitalBorrow_noDigitalVersions_throws() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no digital versions");
    }

    @Test
    @DisplayName("Should throw BadRequestException when user already has active digital borrow")
    void createDigitalBorrow_duplicate_throws() {
        FileResource fr = fileResourceRepository.save(FileResource.builder()
                .originalFilename("book.pdf").storedFilename("uuid.pdf")
                .filePath("digital-books/uuid.pdf")
                .fileSizeBytes(1024L).contentType("application/pdf")
                .checksumSha256("abc789").uploadedBy(adminUser)
                .createdAt(LocalDateTime.now()).build());
        digitalBookRepository.save(DigitalBook.builder()
                .book(book).fileResource(fr).fileFormat("PDF")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build();

        borrowService.createBorrowRequest(library.getId(), book.getId(), req);

        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already has an active digital borrow");
    }

    @Test
    @DisplayName("Physical and digital borrows can coexist for the same book")
    void physicalAndDigitalBorrow_canCoexist() {
        FileResource fr = fileResourceRepository.save(FileResource.builder()
                .originalFilename("book.pdf").storedFilename("uuid2.pdf")
                .filePath("digital-books/uuid2.pdf")
                .fileSizeBytes(1024L).contentType("application/pdf")
                .checksumSha256("coexist123").uploadedBy(adminUser)
                .createdAt(LocalDateTime.now()).build());
        digitalBookRepository.save(DigitalBook.builder()
                .book(book).fileResource(fr).fileFormat("PDF")
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");

        BorrowDTO physical = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build());
        BorrowDTO digital = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.DIGITAL).build());

        assertThat(physical.getBorrowType()).isEqualTo(BorrowType.PHYSICAL);
        assertThat(digital.getBorrowType()).isEqualTo(BorrowType.DIGITAL);
    }

    // ── Approve / Reject / Return ────────────────────────────────────────────

    @Test
    @DisplayName("Should approve physical borrow and mark book copy as BORROWED")
    void approveBorrow_physical_marksCopyBorrowed() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO approved = borrowService.approveBorrowRequest(library.getId(), created.getId());

        assertThat(approved.getStatus()).isEqualTo(BorrowStatus.APPROVED);
        assertThat(approved.getBorrowDate()).isNotNull();
        assertThat(approved.getDueDate()).isNotNull();

        BookCopy copy = bookCopyRepository.findById(approved.getBookCopyId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.BORROWED);
    }

    @Test
    @DisplayName("Should auto-reject other pending borrows when last copy is taken")
    void approveBorrow_autoRejectsOthers_whenNoCopiesLeft() {
        User member2 = userRepository.save(User.builder()
                .email("member2@borrow.com").passwordHash("$2a$10$encoded")
                .firstName("M2").lastName("U").systemRole(SystemRole.USER)
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        membershipRepository.save(LibraryMembership.builder()
                .user(member2).library(library)
                .role(LibraryMembershipRole.MEMBER).status(MembershipStatus.APPROVED)
                .approvedBy(adminUser)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());

        // member1 and member2 both request — only 1 copy exists
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO req1 = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build());

        // member2 needs a different copy slot — create a second copy then request, then remove it
        BookCopy copy2 = bookCopyRepository.save(BookCopy.builder()
                .book(book).library(library).copyNumber(2)
                .status(BookCopyStatus.AVAILABLE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(member2, "USER");
        BorrowDTO req2 = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build());

        // Remove the extra copy so only 1 remains after approval
        copy2.setStatus(BookCopyStatus.BORROWED);
        bookCopyRepository.save(copy2);

        // Approve req1 — the original copy is taken; no more AVAILABLE copies
        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approveBorrowRequest(library.getId(), req1.getId());

        Borrow pending2 = borrowRepository.findById(req2.getId()).orElseThrow();
        assertThat(pending2.getStatus()).isEqualTo(BorrowStatus.REJECTED);
    }

    @Test
    @DisplayName("Should reject borrow request with reason")
    void rejectBorrow_success() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        BorrowDTO rejected = borrowService.rejectBorrowRequest(library.getId(), created.getId(), "Out of policy");

        assertThat(rejected.getStatus()).isEqualTo(BorrowStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Out of policy");
    }

    @Test
    @DisplayName("Should return book and mark copy available again")
    void returnBook_physical_marksCopyAvailable() {
        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO created = borrowService.createBorrowRequest(library.getId(), book.getId(),
                BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build());

        SecurityTestUtils.setSecurityContext(adminUser, "USER");
        borrowService.approveBorrowRequest(library.getId(), created.getId());

        SecurityTestUtils.setSecurityContext(memberUser, "USER");
        BorrowDTO returned = borrowService.returnBook(library.getId(), created.getId());

        assertThat(returned.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(returned.getReturnDate()).isNotNull();

        BookCopy copy = bookCopyRepository.findById(created.getBookCopyId()).orElseThrow();
        assertThat(copy.getStatus()).isEqualTo(BookCopyStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when non-member requests borrow")
    void createBorrow_nonMember_throwsUnauthorized() {
        User outsider = userRepository.save(User.builder()
                .email("outsider@borrow.com").passwordHash("$2a$10$encoded")
                .firstName("Out").lastName("Sider")
                .systemRole(SystemRole.USER).accountStatus(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build());
        SecurityTestUtils.setSecurityContext(outsider, "USER");

        BorrowRequest req = BorrowRequest.builder().borrowType(BorrowType.PHYSICAL).build();
        assertThatThrownBy(() -> borrowService.createBorrowRequest(library.getId(), book.getId(), req))
                .isInstanceOf(UnauthorizedException.class);
    }
}
